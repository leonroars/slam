# API Design Specification

## Overview

This document outlines the key architectural decisions and design patterns used in the Concert Reservation System's API. The API is designed to handle **high concurrency**, ensure **data integrity**, and manage **traffic surges** effectively.

---

## Core Design Patterns

### 1. Token-Based Flow Control (Queue)
To protect the backend from overwhelming traffic during reservation openings, a **Token-Based Queue Mechanism** is implemented.
- **Wait vs. Active Tokens**: Users first receive a `WAIT` token. The scheduler periodically activates a batch of tokens to `ACTIVE` status based on available server capacity (`maxConcurrentUser`).
- **HttpOnly Cookies**: Tokens are issued effectively as `HttpOnly` cookies to prevent XSS attacks and simplify client-side management.
- **Redis TTL**: Tokens have a 12-hour TTL in Redis to automatically clear stale states.

### 2. Idempotency & Reliability
The reservation creation endpoint is critical and sensitive to network failures.
- **Idempotency Key**: The `POST /api/reservations` endpoint accepts an idempotency key to prevent duplicate charges or double-bookings if the client retries a timed-out request.
- **Transactional Outbox**: Critical side effects (like data platform analytics) are handled via domain events published to an `OUTBOX` table, ensuring eventual consistency even if the message broker is temporarily down.

### 3. Concurrency Control
- **Optimistic Locking**: Point balances use `@Version` based optimistic locking. This is chosen over pessimistic locking to avoid database deadlocks during high-concurrency charge/use operations, as conflicts are expected to be rare for a single user's balance.

---

## Key API Specifications

### 1. Issue Queue Token
**POST** `/api/queue/tokens`

Issues a queue token. If the system is under heavy load, the token starts in `WAIT` status.

**Request**
- `userId` (Query Param): User Identifier
- `scheduleId` (Query Param): Target Concert Schedule

**Response (200 OK)**
- **Cookie**: `tokenId` (HttpOnly, Secure)
- **Body**:
```json
{
  "queueStatus": "WAIT", // or "ACTIVE"
  "estimatedWaitTime": 120, // seconds, optional
  "serverTime": "2024-01-01T12:00:00"
}
```

### 2. Create Reservation (Temporary)
**POST** `/api/reservations`

Temporarily assigns a seat (Preempted). The seat is held for 5 minutes.

**Request**
- Header `Idempotency-Key`: `"uuid-v4"`
- Body:
```json
{
  "userId": 101,
  "concertScheduleId": 500,
  "seatId": 25,
  "price": 150000
}
```

**Response (200 OK)**
```json
{
  "reservationId": "TSID-12345",
  "status": "PREEMPTED",
  "expiredAt": "2024-01-01T12:05:00",
  "seatNumber": 25
}
```

### 3. Use Points
**POST** `/api/users/{userId}/point/use`

Deducts points for payment. Uses optimistic locking.

**Request**
```json
{
  "amount": 150000
}
```

**Response (200 OK)**
```json
{
  "userId": 101,
  "balance": 50000
}
```
