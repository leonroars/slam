-- ------------------------------------------------------------
-- Global defaults
-- ------------------------------------------------------------
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================
-- 1) USER (extends BaseJpaEntity)
-- ============================================================
CREATE TABLE IF NOT EXISTS `USER` (
                                      `user_id`    CHAR(36)     NOT NULL,
    `name`       VARCHAR(255) NOT NULL,
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`user_id`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 2) USERPOINTBALANCE (extends BaseJpaEntity)
--    요구사항: AUTO_INCREMENT ID 필드 사용
-- ============================================================
CREATE TABLE IF NOT EXISTS `USERPOINTBALANCE` (
                                                  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
                                                  `user_id`    CHAR(36)     NOT NULL,
    `balance`    INT          NOT NULL DEFAULT 0,
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `IDX_USERPOINTBALANCE_USER` (`user_id`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 3) POINTHISTORY  (BaseJpaEntity 아님)
--    PointTransactionType: CHARGE, USE, INIT
-- ============================================================
CREATE TABLE IF NOT EXISTS `POINTHISTORY` (
                                              `point_history_id` CHAR(36)                         NOT NULL,
    `userId`           CHAR(36)                         NOT NULL,
    `point`            INT                              NOT NULL,
    `transactionType`  ENUM('CHARGE','USE','INIT')      NOT NULL,
    `description`      TEXT                             NULL,
    `transactionDate`  DATETIME(6)                      NOT NULL,
    PRIMARY KEY (`point_history_id`),
    INDEX `IDX_POINTHISTORY_USERID_DATE` (`userId`, `transactionDate`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 4) CONCERT  (extends BaseJpaEntity)
-- ============================================================
CREATE TABLE IF NOT EXISTS `CONCERT` (
                                         `concertId`  CHAR(36)     NOT NULL,
    `name`       VARCHAR(255) NOT NULL,
    `artist`     VARCHAR(255) NOT NULL,
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`concertId`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 5) CONCERTSCHEDULE  (extends BaseJpaEntity)
--    ConcertScheduleAvailability: AVAILABLE, SOLDOUT
--    @Index("reservationStartAt, reservationEndAt")
-- ============================================================
CREATE TABLE IF NOT EXISTS `CONCERTSCHEDULE` (
                                                 `concert_schedule_id` CHAR(36)                        NOT NULL,
    `concertId`           CHAR(36)                        NOT NULL,
    `availability`        ENUM('AVAILABLE','SOLDOUT')     NOT NULL,
    `datetime`            DATETIME(6)                     NOT NULL,
    `reservationStartAt`  DATETIME(6)                     NOT NULL,
    `reservationEndAt`    DATETIME(6)                     NOT NULL,
    `created_at`          DATETIME(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`          DATETIME(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`concert_schedule_id`),
    INDEX `IDX_RESERVATION_AVAILABLE_PERIOD` (`reservationStartAt`, `reservationEndAt`),
    INDEX `IDX_CONCERTSCHEDULE_CONCERTID` (`concertId`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 6) SEAT  (extends BaseJpaEntity)
--    SeatStatus: AVAILABLE, UNAVAILABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS `SEAT` (
                                      `seatId`            CHAR(36)                         NOT NULL,
    `concertScheduleId` CHAR(36)                         NOT NULL,
    `number`            INT                              NOT NULL,
    `price`             INT                              NOT NULL,
    `status`            ENUM('AVAILABLE','UNAVAILABLE')  NOT NULL,
    `created_at`        DATETIME(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`        DATETIME(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`seatId`),
    UNIQUE KEY `UK_SEAT_SCHEDULE_NUMBER` (`concertScheduleId`, `number`),
    INDEX `IDX_SEAT_CONCERT_SCHEDULE_ID` (`concertScheduleId`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 7) QUEUE (TokenJpaEntity)  (extends BaseJpaEntity)
--    TokenStatus: WAIT, ACTIVE, EXPIRED
-- ============================================================
CREATE TABLE IF NOT EXISTS `QUEUE` (
                                       `id`                BIGINT                            NOT NULL AUTO_INCREMENT,
                                       `concertScheduleId` CHAR(36)                          NOT NULL,
    `userId`            CHAR(36)                          NOT NULL,
    `status`            ENUM('WAIT','ACTIVE','EXPIRED')   NOT NULL DEFAULT 'WAIT',
    `expiredAt`         DATETIME(6)                       NULL,
    `created_at`        DATETIME(6)                       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`        DATETIME(6)                       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `IDX_QUEUE_SCHEDULE_STATUS_CREATED` (`concertScheduleId`, `status`, `created_at`),
    INDEX `IDX_QUEUE_USER` (`userId`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 8) RESERVATION  (extends BaseJpaEntity)
--    ReservationStatus: PAID, BOOKED, CANCELLED, EXPIRED
-- ============================================================
CREATE TABLE IF NOT EXISTS `RESERVATION` (
                                             `reservation_id`    CHAR(36)                                   NOT NULL,
    `userId`            CHAR(36)                                   NOT NULL,
    `concertScheduleId` CHAR(36)                                   NOT NULL,
    `seatId`            CHAR(36)                                   NOT NULL,
    `price`             INT                                        NOT NULL,
    `status`            ENUM('PAID','BOOKED','CANCELLED','EXPIRED') NOT NULL,
    `expiredAt`         DATETIME(6)                                NULL,
    `created_at`        DATETIME(6)                                NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`        DATETIME(6)                                NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`reservation_id`),
    INDEX `IDX_RESERVATION_USER` (`userId`),
    INDEX `IDX_RESERVATION_SCHEDULE` (`concertScheduleId`),
    INDEX `IDX_RESERVATION_SEAT` (`seatId`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;