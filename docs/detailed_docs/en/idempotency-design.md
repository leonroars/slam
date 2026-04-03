# ADR: AOP-Based Idempotency Module with Redis Response Caching

## Status

Accepted

---

## Context

The Concert Reservation System exposes non-idempotent mutation endpoints (e.g., `POST /reservations`) that can be invoked multiple times due to client retries, network failures, or user double-clicks. Without server-side idempotency guarantees:

1. **Duplicate side effects**: A retried reservation request could create two reservations for the same seat, double-charge points, or produce inconsistent state across domains.
2. **Concurrency under retries**: When multiple identical requests arrive concurrently, all of them may pass validation and execute the business logic simultaneously, leading to race conditions that domain-level locking alone may not cover.
3. **Cross-domain applicability**: The system has multiple domains (Reservation, Payment, Point) that each expose mutation endpoints. An idempotency mechanism scoped to a single domain would need to be duplicated across all of them.

The module must:
- Prevent duplicate execution of business logic for the same logical request.
- Handle concurrent duplicate requests without blocking server resources on non-promising work.
- Be reusable across all domains with minimal integration effort.

---

## Decision

### 1. Annotation-Driven AOP Design (`@Idempotent` + `@Aspect`)

The module is implemented as a Spring AOP aspect (`IdempotencyAspect`) that intercepts methods annotated with `@Idempotent`.

```java
@PostMapping("/reservations")
@Idempotent(operationKey = "reservation.create")
public ResponseEntity<ReservationResponse> createTemporaryReservation(...) { ... }
```

**Why AOP over Servlet Filter / HandlerInterceptor:**

- The idempotency module interacts with Redis-backed storage and Redisson locks — Spring-managed beans that are most naturally accessed within the Spring bean proxy lifecycle via constructor injection.
- `@Aspect` provides direct access to the target method's annotation metadata (`operationKey`, `responseCacheDuration`, `responseCacheTimeUnit`) without needing to manually resolve the handler method via `HandlerMapping`.
- AOP composes cleanly with `@Transactional` via `@Order(Ordered.HIGHEST_PRECEDENCE)`, ensuring the idempotency check runs **before** any transaction is opened. This prevents a duplicate request from acquiring a database connection or starting a transaction that will ultimately be discarded.

### 2. Client-Provided Idempotency Key via HTTP Header

The client must provide an `Idempotency-Key` HTTP header with each request. The key is combined with the annotation's `operationKey` to form the Redis cache key:

```
IDEMPOTENCY:RESULT:{operationKey}:{idempotencyKey}
```

If the header is missing or blank, the request is rejected immediately. This places key generation responsibility on the client, which is the only party that knows whether a request is a retry or a genuinely new operation.

### 3. Non-Blocking Lock with Immediate 202 Response for Duplicates

When no cached result exists, the aspect attempts a **non-blocking** `tryLock()` via Redisson:

- **Lock acquired**: Execute business logic, cache the `ResponseEntity` in Redis, return the result.
- **Lock not acquired** (another request with the same key is already in progress): Return `202 Accepted` immediately without executing any business logic.

**Rationale:** A duplicate request that arrives while the original is still processing is inherently non-promising — the original has already been accepted, and making the duplicate wait would:
- Allocate a Tomcat thread to a request that will never produce a new result.
- Hold server resources (thread, connection) for an indeterminate duration.
- Provide no additional value to the client, since the original request's result will be available via cache on the next retry.

Returning immediately preserves server throughput and thread availability — key performance indicators for this system.

> **Known trade-off:** The current implementation includes a `Retry-After: 5` header in the 202 response. This is semantically imprecise — `Retry-After` implies the client should retry the same request, while the intent is closer to "your request is already being processed, please wait." A future revision may replace this with a body message or a different signaling mechanism.

### 4. Redisson Distributed Lock with Watchdog TTL Management

The module uses Redisson's `RLock` API instead of a raw Redis `SETNX`-based lock.

**Evolution from SETNX:** The initial implementation used `SETNX` with a physical TTL for the lock. This introduced a race condition: if the business logic took longer than the lock TTL, the lock would expire while the logic was still executing, allowing a second request to acquire the lock and execute the same operation concurrently.

**Why Redisson:** Redisson's lock API provides a **watchdog** feature that automatically extends the lock TTL as long as the holding thread is alive. This eliminates the TTL-vs-execution-time race condition without requiring manual TTL tuning per endpoint.

**Infinite lock safety:** The watchdog keeps the lock alive as long as the thread is active, which could theoretically hold a lock indefinitely. This is considered safe in this system because:
- The module is designed to wrap domain business logic that operates purely on internal databases — no third-party API calls or external I/O that could block a thread indefinitely.
- Each domain is responsible for its own data consistency guarantees (e.g., optimistic locking, timeouts). The idempotency module delegates this responsibility rather than imposing its own lock timeout policy, keeping the module domain-agnostic.

### 5. Response Caching via Redis with Codec

Successful responses (`ResponseEntity<T>`) are serialized into an `IdempotencyRecord` and stored in Redis with a configurable TTL (default: 12 hours in `IdempotencyStorageService`).

The `ResponseIdempotencyCodec` handles encode/decode:
- **Encode**: Serializes the response body to JSON, stores the HTTP status code as `int`, and records the body's FQCN (Fully Qualified Class Name) for deserialization.
- **Decode**: Reconstructs the `ResponseEntity` from the cached record using `Class.forName()` to resolve the body type.

The `IdempotencyRecord` tracks a status field (`PROCESSING`, `COMPLETED`) and a `createdAt` timestamp for debugging.

> **Known issue — `bodyType` FQCN storage:** The `bodyType` field stores the response body's class name as a string in Redis. This was originally identified as a security concern (Jackson Deserialization Gadget attack vector — an attacker who can write to Redis could inject an arbitrary class name). Removal of `bodyType` in favor of resolving the type from the handler method's return type was planned but not yet applied.

> **Known limitation — generic type erasure:** When the response body is a generic collection (e.g., `List<SomeDto>`), Java type erasure causes the concrete collection implementation class to be stored (e.g., `ImmutableCollections$ListN`) rather than the parameterized type. This means round-trip decode for collection-typed responses may lose element type information.

### 6. Aspect Ordering

`@Order(Ordered.HIGHEST_PRECEDENCE)` ensures the idempotency aspect executes before all other aspects, critically before `@Transactional`. This guarantees that:
- A cached response is returned without opening a database transaction.
- A `202 Accepted` for a duplicate-in-progress is returned without acquiring any database resources.

---

## Alternatives Considered

### Servlet Filter

- Simpler request/response interception model.
- Rejected because Servlet Filters operate outside the Spring bean lifecycle, making it difficult to inject Spring-managed dependencies (Redisson, Redis templates). Additionally, resolving handler method annotations from a filter requires manually invoking `HandlerMapping`, adding unnecessary complexity.

### HandlerInterceptor

- Operates within Spring MVC and has access to the `HandlerMethod`.
- Rejected because interceptors split pre/post logic across `preHandle` and `postHandle`, making the atomic "check-execute-cache" flow harder to express cleanly compared to AOP's `@Around` advice. Also, interceptors lack native support for annotation-driven configuration per method.

### Blocking Lock (`lock()` Instead of `tryLock()`)

- Would allow the duplicate request to wait and eventually return the cached result directly, providing a simpler client experience.
- Rejected because blocking ties up a Tomcat thread for a non-promising request. Under high concurrency, this could exhaust the thread pool with waiting requests, degrading availability for genuinely new requests — a worse outcome than returning 202 immediately.

### Database-Based Idempotency (Unique Constraint on Idempotency Key)

- Store idempotency records in the primary database with a unique constraint.
- Rejected because the idempotency module is designed to **protect** the database from duplicate traffic. Routing idempotency checks through the database would defeat this purpose and add load to the resource it aims to shield.

---

## Consequences

### Positive

- **Domain-agnostic reuse**: Any controller method can become idempotent by adding `@Idempotent(operationKey = "...")` — no domain-specific code changes required.
- **Database protection**: Duplicate requests are intercepted at the Redis layer before reaching the database, preserving database throughput under retry storms.
- **Thread efficiency**: Non-blocking lock with immediate 202 prevents thread pool exhaustion from queued duplicate requests.
- **Race condition safety**: Redisson watchdog eliminates the lock-TTL-vs-execution-time race that plagued the SETNX-based approach.
- **Transparent caching**: The response codec preserves the full HTTP semantics (status code + body) of the original response, making cached responses indistinguishable from live ones to the client.

### Negative / Trade-offs

- **Redis dependency**: The module requires Redis (for caching) and Redisson (for distributed locks) as infrastructure dependencies. Redis unavailability would cause idempotency checks to fail, potentially blocking all annotated endpoints.
- **FQCN security concern**: Storing `bodyType` as a class name in Redis is a potential deserialization gadget vector. This should be addressed by resolving the type from the handler method's return type instead.
- **Generic collection limitation**: Responses with generic collection bodies may not round-trip correctly due to Java type erasure.
- **202 semantics**: The `Retry-After` header on 202 responses is semantically imprecise for "your request is already in progress." This may confuse clients that interpret 202 + Retry-After as a standard async polling pattern.
- **Watchdog reliance**: Correctness depends on the assumption that business logic completes in bounded time without indefinite blocking. Introducing external API calls in annotated methods would violate this assumption and risk indefinite lock holding.
