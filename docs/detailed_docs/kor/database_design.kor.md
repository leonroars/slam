# 데이터베이스 스키마 설계

## 개요

본 시스템은 콘서트 좌석 예약 플랫폼을 지원하기 위해 4개 논리적 도메인에 걸쳐 **7개 테이블**을 사용합니다. 스키마는 두 가지 제약 조건을 중심으로 설계되었습니다: **동시 접근 환경에서의 데이터 정합성 보장**과 각 도메인을 독립적으로 발전시킬 수 있도록 하는 **도메인 간 느슨한 결합**입니다.

![Schema Diagram](../../images/slam_schema_JPA.png)

---

## 설계 결정 사항

### 1. 물리적 외래 키 미사용

모든 테이블 간 참조는 DDL에 `FOREIGN KEY` 제약조건 없이 **논리적 참조**(애플리케이션 레벨 TSID 값)로 구현했습니다. 이는 의도적인 설계 결정입니다:

- 각 도메인(User, Concert, Reservation, Queue)이 자체 테이블을 소유하며, 스키마 수준의 결합 없이 독립적으로 진화하거나 물리적으로 분리할 수 있습니다.
- 참조 무결성은 데이터베이스가 아닌 애플리케이션/도메인 계층에서 보장합니다.
- 고아 참조(orphaned reference) 발생 가능성을 감수하는 대신, 도메인 독립성과 운영 유연성을 확보하는 트레이드오프입니다.

### 2. TSID 기본 키 전략

대부분의 테이블은 애플리케이션에서 생성하는 **TSID**(Time-Sorted ID, `io.hypersistence.tsid`)를 `BIGINT`로 저장합니다:

- **B-tree 친화적**: TSID는 단조 증가하므로, 랜덤 UUID가 InnoDB 클러스터드 인덱스에서 유발하는 랜덤 쓰기 패턴과 페이지 분할을 방지합니다.
- **저장 효율**: 128비트 UUID 대비 64비트 `BIGINT`로 인덱스 크기를 절감합니다.
- **애플리케이션 생성 ID**: 영속화 이전에 ID가 할당되므로, 엔티티 저장 전에도 도메인 모델에서 참조가 가능합니다.

예외: `POINTBALANCE`와 `QUEUE`는 TSID 대신 데이터베이스 `AUTO_INCREMENT`를 사용합니다.

### 3. 포인트 잔액 낙관적 락

`POINTBALANCE`는 JPA `@Version` 필드를 통한 낙관적 동시성 제어를 적용합니다. 두 개의 동시 트랜잭션이 동일 사용자의 잔액을 수정하려 할 경우, 하나는 `OptimisticLockException`으로 실패하며 재시도할 수 있습니다. 이를 통해 트랜잭션 수행 중 데이터베이스 레벨의 락을 점유하지 않습니다.

### 4. 감사(Auditing)

`BaseJpaEntity` Mapped Superclass가 Spring Data JPA의 `@CreatedDate` / `@LastModifiedDate` 감사 기능을 통해 `created_at`과 `updated_at`을 제공합니다. `POINTHISTORY`를 제외한 모든 테이블이 이 베이스 엔티티를 상속합니다.

### 5. Enum 저장 방식

도메인 Enum(`SeatStatus`, `ReservationStatus`, `TokenStatus` 등)은 ordinal 정수가 아닌 `VARCHAR` 문자열로 저장합니다. 이를 통해 원시 데이터의 가독성을 확보하고, Enum 순서 변경으로 인해 저장된 값이 깨지는 문제를 방지합니다.

### 6. 인덱싱 전략

| 테이블 | 인덱스 | 컬럼 | 목적 |
|-------|--------|------|------|
| `CONCERTSCHEDULE` | `IDX_RESERVATION_START_AT` | `reservationStartAt` | 예약 가능 기간 조회 시 스캔 범위를 축소합니다. 도메인 특성상 동시 예약 가능한 스케줄이 현실적으로 수백 건 수준이므로, `start_at` 단일 컬럼 인덱스만으로도 복합 인덱스 오버헤드 없이 충분한 선택도를 확보할 수 있습니다. |
| `SEAT` | `IDX_SEAT_CONCERT_SCHEDULE_ID` | `concertScheduleId` | 특정 스케줄에 대한 전체 좌석 조회에 사용됩니다. |

---
## 주요 테이블 명세

### User 도메인

#### `POINTBALANCE`
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `point_balance_id` | BIGINT | PK, AUTO_INCREMENT | |
| `user_id` | BIGINT | | `USER.user_id`에 대한 논리적 참조 |
| `point` | INT | | 현재 잔액 (0 ~ 1,000,000) |
| `version` | BIGINT | | 낙관적 락을 위한 `@Version` |
| `created_at` | DATETIME(6) | NOT NULL, auto | JPA 감사 |
| `updated_at` | DATETIME(6) | auto | JPA 감사 |

- 동시 포인트 충전/사용 작업에서 비관적 락 없이 처리하기 위해 JPA `@Version`을 통한 **낙관적 락**을 적용합니다.
- 잔액 범위(0 ~ 1,000,000)는 데이터베이스 제약조건이 아닌 도메인 모델 레벨(`Point` Value Object)에서 검증합니다.

#### `POINTHISTORY`
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `point_history_id` | BIGINT | PK (TSID) | 애플리케이션 생성 TSID |
| `user_id` | BIGINT | | `USER.user_id`에 대한 논리적 참조 |
| `point` | INT | | 거래 금액 |
| `transaction_type` | VARCHAR(255) | | `CHARGE`, `USE`, `INIT` |
| `description` | VARCHAR(255) | | |
| `transaction_date` | DATETIME(6) | | |

- 포인트 거래의 추가 전용(append-only) 이력 로그입니다.
- `BaseJpaEntity`를 상속하지 않으며(`created_at` / `updated_at` 감사 미적용), 자체 `transaction_date` 필드를 사용합니다.

---

### Concert 도메인

#### `CONCERTSCHEDULE`
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `concert_schedule_id` | BIGINT | PK (TSID) | 애플리케이션 생성 TSID |
| `concert_id` | BIGINT | | `CONCERT.concert_id`에 대한 논리적 참조 |
| `availability` | VARCHAR(255) | | `AVAILABLE`, `SOLDOUT` |
| `datetime` | DATETIME(6) | | 공연 일시 |
| `reservation_start_at` | DATETIME(6) | 복합 인덱스 | 예약 오픈 시각 |
| `reservation_end_at` | DATETIME(6) | 복합 인덱스 | 예약 마감 시각 |
| `created_at` | DATETIME(6) | NOT NULL, auto | JPA 감사 |
| `updated_at` | DATETIME(6) | auto | JPA 감사 |

- **인덱스 `IDX_RESERVATION_START_AT`**: 예약 가능 기간 내 스케줄을 필터링하는 범위 조회를 지원합니다.

#### `SEAT`
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `seat_id` | BIGINT | PK (TSID) | 애플리케이션 생성 TSID |
| `concert_schedule_id` | BIGINT | 인덱스 | `CONCERTSCHEDULE`에 대한 논리적 참조 |
| `number` | INT | | 스케줄 내 좌석 번호 |
| `price` | INT | | 좌석 가격 |
| `status` | VARCHAR(255) | | `AVAILABLE`, `UNAVAILABLE` |
| `created_at` | DATETIME(6) | NOT NULL, auto | JPA 감사 |
| `updated_at` | DATETIME(6) | auto | JPA 감사 |

- **인덱스 `IDX_SEAT_CONCERT_SCHEDULE_ID`**: `(concertScheduleId)` — 특정 스케줄의 전체 좌석을 조회할 때 사용됩니다.

---

### Reservation 도메인

#### `RESERVATION`
| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `reservation_id` | BIGINT | PK (TSID) | 애플리케이션 생성 TSID |
| `user_id` | BIGINT | | `USER.user_id`에 대한 논리적 참조 |
| `concert_schedule_id` | BIGINT | | `CONCERTSCHEDULE`에 대한 논리적 참조 |
| `seat_id` | BIGINT | | `SEAT`에 대한 논리적 참조 |
| `price` | INT | | 결제 금액 |
| `status` | VARCHAR(255) | | 아래 상태 머신 참조 |
| `expired_at` | DATETIME(6) | | 임시 선점 예약의 만료 기한 |
| `created_at` | DATETIME(6) | NOT NULL, auto | JPA 감사 |
| `updated_at` | DATETIME(6) | auto | JPA 감사 |

**예약 상태 머신:**
```
PREEMPTED ──→ PAYMENT_PENDING ──→ CONFIRMED ──→ CANCELLED
    │
    └──→ EXPIRED
```

- `PREEMPTED`: 좌석이 임시 선점된 상태입니다. `expired_at`은 `created_at + 5분`으로 설정됩니다.
- `PAYMENT_PENDING`: 결제 프로세스가 시작된 상태입니다. 비순서 보장(non-FIFO) 메시지 큐 사용 시 메시지 순서 역전에 대비하기 위한 중간 상태입니다.
- `CONFIRMED`: 결제가 완료된 상태입니다.
- `EXPIRED`: 5분 선점 시간이 결제 없이 만료된 상태입니다. 스케줄러가 해당 좌석을 회수합니다.
- `CANCELLED`: 확정된 예약이 취소된 상태입니다.
- 상태 간 전이 규칙은 POJO 도메인 모델 내의 코드 수준에서의 정의 및 검증을 통해 강제됩니다.
