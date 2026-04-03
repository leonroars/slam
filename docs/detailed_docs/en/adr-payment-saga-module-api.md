# ADR: Payment Domain with Saga/Compensation Pattern and Inter-Domain Module APIs

## Status

Accepted

---

## Context

The Concert Reservation System requires payment processing that coordinates multiple domains — **Payment**, **Point**, and **Reservation** — within a single business transaction. Specifically:

- **Processing a payment** involves: deducting user points, confirming a reservation, and recording the payment.
- **Processing a refund** involves: restoring user points, cancelling a reservation, and recording the refund.

These cross-domain operations introduce the following challenges:

1. **Partial failure**: Any step in the multi-step flow can fail independently. Without proper handling, the system may end up in an inconsistent state (e.g., points deducted but reservation not confirmed).
2. **Synchronous user expectation**: Payment is a user-facing operation where the user expects a deterministic result immediately upon request. The coordination mechanism must be able to return a definitive success or failure within the same request-response cycle.
3. **Tight cross-domain coupling**: Prior to this change, domains called each other's services directly, creating hidden dependencies and making it difficult to reason about failure boundaries.
4. **Transient failures under concurrency**: Point balance operations use optimistic locking, which is prone to transient conflicts during high-concurrency scenarios.

---

## Decision

### 1. Saga/Compensation Pattern for Payment Orchestration

The `PaymentOrchestrator` coordinates multi-step payment and refund flows using the **orchestration-based saga pattern**.

**Payment Flow:**
1. Create a `PENDING` payment record.
2. Decrease user point balance via `PointModuleApi`.
3. On failure at step 2: mark payment as `FAILED` and return (no compensation needed — no side effect has occurred).
4. Confirm reservation via `ReservationModuleApi`.
5. On success: mark payment as `COMPLETED`.
6. On failure at step 4: execute compensating transaction (restore points), mark payment as `FAILED`.
7. If compensation itself fails: persist a `CompensationTxLog` entry for deferred retry.

**Refund Flow:**
1. Create a `PENDING` payment record.
2. Increase user point balance via `PointModuleApi`.
3. On failure at step 2: mark payment as `FAILED` and return (no compensation needed).
4. Cancel reservation via `ReservationModuleApi`.
5. On success: mark payment as `REFUNDED`.
6. On failure at step 4: execute compensating transaction (deduct restored points), mark payment as `FAILED`.
7. If compensation itself fails: persist a `CompensationTxLog` entry with negative price for deferred retry.

A `CompensationTxRetryScheduler` runs on a fixed 60-second interval, picking up all `PENDING` compensation logs and retrying them up to a maximum of 3 attempts.

### 2. Module API Layer for Inter-Domain Communication

Each domain exposes a public contract through a **Module API** interface:

- `PointModuleApi` — declares `decreaseUserPointBalance()` and `increaseUserPointBalance()`
- `ReservationModuleApi` — declares `confirmReservation()` and `cancelReservation()`

Each interface is accompanied by:
- An **operation result record** (`PointOperationResult`, `ReservationOperationResult`) that wraps success/failure status and error codes.
- A **facade implementation** (`PointModuleFacade`, `ReservationModuleFacade`) that delegates to internal domain services and translates exceptions into result objects.

Callers (e.g., `PaymentOrchestrator`) depend **only on the interface**, not on concrete service classes. This enforces a clear boundary between domains and makes it straightforward to evolve each domain independently.

### 3. Retry with Exponential Backoff (Point Domain)

`PointModuleFacade` applies Spring `@Retryable` to handle transient failures:

| Configuration       | Value                                   |
|---------------------|-----------------------------------------|
| Max attempts        | 3                                       |
| Initial delay       | 100 ms                                  |
| Multiplier          | 2.0 (100ms → 200ms → 400ms)            |
| Excluded exceptions | `BusinessRuleViolationException`, `OptimisticLockingFailureException` |

Non-retryable exceptions (business rule violations, optimistic lock failures) are returned immediately as a failed result via the `@Recover` method, avoiding unnecessary retry on deterministic failures.

### 4. Append-Only Payment Model

The `Payment` domain model follows an **append-only** design:

- No setters — state transitions produce a new instance via `withStatus()`.
- Each status change is persisted as a new record, preserving a complete audit trail.
- Statuses: `PENDING` → `COMPLETED` | `FAILED` | `REFUNDED`.

---

## Alternatives Considered

### Choreography-Based Saga (Event-Driven)
- Each domain publishes events and reacts to events from other domains. No central coordinator — domains are fully decoupled.
- Rejected because payment processing requires a **synchronous, deterministic response** to the user. In a choreography saga, the outcome is resolved asynchronously through event propagation, making it impossible to return a definitive success or failure within the same request-response cycle. The orchestration approach keeps the entire flow within a single synchronous call, giving the user immediate feedback on their payment or refund request.

### Direct Service-to-Service Calls (No API Layer)
- Simpler to implement — just inject and call the target domain's service directly.
- Rejected because it creates hidden cross-domain dependencies, makes failure handling inconsistent, and complicates future migration to a distributed architecture (e.g., microservices).

### Fail-Fast Without Retry
- Simpler to reason about — every failure is immediately surfaced to the caller.
- Rejected for the Point domain because optimistic lock conflicts under concurrent reservations are **expected** transient failures. A brief exponential backoff significantly improves success rates without adding meaningful latency.

---

## Consequences

### Positive

- **Clear domain boundaries**: Module API interfaces serve as explicit contracts. Cross-domain dependencies are visible and auditable in the interface definitions.
- **Fault-tolerant payment flows**: The compensation pattern ensures the system recovers from partial failures automatically, with scheduled retry as a safety net.
- **Audit trail**: Append-only payment records provide a complete history of state transitions for debugging and compliance.
- **Retry resilience**: Point operations gracefully handle transient concurrency conflicts without propagating failures to the user.

### Negative / Trade-offs

- **Eventual consistency**: When compensation is deferred to `CompensationTxLog`, the system is temporarily inconsistent (up to 60 seconds per retry cycle, maximum 3 attempts). Monitoring and alerting on failed compensation logs is operationally required.
- **Operational overhead**: The `CompensationTxRetryScheduler` and `CompensationTxLog` table require monitoring to detect cases where all retry attempts are exhausted (manual intervention needed).
- **Asymmetric retry**: Only `PointModuleFacade` implements retry logic. `ReservationModuleFacade` uses simple exception handling — a single transient failure at the reservation step will trigger the compensation path rather than retrying in-place.
