# API 설계 명세

## 개요

본 문서는 콘서트 예약 시스템 API의 주요 아키텍처 결정 사항과 설계 패턴을 정리합니다. API는 **높은 동시성 처리**, **데이터 정합성 보장**, **트래픽 급증 대응**을 핵심 목표로 설계되었습니다.

---

## 핵심 설계 패턴

### 1. 토큰 기반 흐름 제어 (대기열)
예약 오픈 시 백엔드에 가해지는 과도한 트래픽으로부터 시스템을 보호하기 위해 **토큰 기반 대기열 메커니즘**을 적용했습니다.
- **대기(WAIT) / 활성(ACTIVE) 토큰**: 사용자는 최초 `WAIT` 상태의 토큰을 발급받습니다. 스케줄러가 서버 수용 가능 인원(`maxConcurrentUser`)에 따라 주기적으로 일정량의 토큰을 `ACTIVE` 상태로 전환합니다.
- **HttpOnly 쿠키**: XSS 공격 방지와 클라이언트 측 관리 간소화를 위해 토큰을 `HttpOnly` 쿠키로 발급합니다.
- **Redis TTL**: Redis에 12시간 TTL을 설정하여 만료된 토큰 상태를 자동으로 정리합니다.

### 2. 멱등성 및 신뢰성
예약 생성 엔드포인트는 네트워크 장애에 민감한 핵심 API입니다.
- **멱등성 키(Idempotency Key)**: `POST /api/reservations` 엔드포인트는 멱등성 키를 통해, 클라이언트가 타임아웃으로 인해 요청을 재시도하더라도 중복 결제나 이중 예약이 발생하지 않도록 보장합니다.
- **Transactional Outbox**: 데이터 플랫폼 분석 등 핵심 부수 효과(side effect)는 도메인 이벤트를 `OUTBOX` 테이블에 발행하는 방식으로 처리하여, 메시지 브로커가 일시적으로 중단되더라도 최종적 일관성(Eventual Consistency)을 보장합니다.

### 3. 동시성 제어
- **낙관적 락(Optimistic Locking)**: 포인트 잔액은 `@Version` 기반 낙관적 락을 적용합니다. 단일 사용자의 잔액에 대한 충돌 빈도가 낮을 것으로 예상되므로, 높은 동시성의 충전/사용 작업에서 데이터베이스 데드락을 유발할 수 있는 비관적 락 대신 낙관적 락을 선택했습니다.

---

## 주요 API 명세

### 1. 대기열 토큰 발급
**POST** `/api/queue/tokens`

대기열 토큰을 발급합니다. 시스템에 부하가 집중된 경우, 토큰은 `WAIT` 상태로 발급됩니다.

**요청**
- `userId` (Query Param): 사용자 식별자
- `scheduleId` (Query Param): 대상 공연 스케줄

**응답 (200 OK)**
- **Cookie**: `tokenId` (HttpOnly, Secure)
- **Body**:
```json
{
  "queueStatus": "WAIT", // 또는 "ACTIVE"
  "estimatedWaitTime": 120, // 초 단위, 선택적
  "serverTime": "2024-01-01T12:00:00"
}
```

### 2. 예약 생성 (임시 선점)
**POST** `/api/reservations`

좌석을 임시 선점(Preempted)합니다. 선점된 좌석은 5분간 유지됩니다.

**요청**
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

**응답 (200 OK)**
```json
{
  "reservationId": "TSID-12345",
  "status": "PREEMPTED",
  "expiredAt": "2024-01-01T12:05:00",
  "seatNumber": 25
}
```

### 3. 포인트 사용
**POST** `/api/users/{userId}/point/use`

결제를 위한 포인트를 차감합니다. 낙관적 락이 적용됩니다.

**요청**
```json
{
  "amount": 150000
}
```

**응답 (200 OK)**
```json
{
  "userId": 101,
  "balance": 50000
}
```