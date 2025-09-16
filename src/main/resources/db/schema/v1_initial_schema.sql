-- ------------------------------------------------------------
-- Global defaults
-- ------------------------------------------------------------
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================
-- 1) USER
-- ============================================================
CREATE TABLE IF NOT EXISTS `USER` (
    `user_id`    VARCHAR(255) NOT NULL,
    `name`       VARCHAR(255) NOT NULL,
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2) USERPOINTBALANCE
-- ============================================================
CREATE TABLE IF NOT EXISTS `USERPOINTBALANCE` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`    VARCHAR(255)  NOT NULL,
    `balance`    INT           NOT NULL DEFAULT 0,
    `created_at` DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `IDX_USERPOINTBALANCE_USER` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3) POINTHISTORY
-- ============================================================
CREATE TABLE IF NOT EXISTS `POINTHISTORY` (
    `point_history_id` VARCHAR(255) NOT NULL,
    `userId`           VARCHAR(255) NOT NULL,
    `point`            INT          NOT NULL,
    `transactionType`  VARCHAR(255) NOT NULL,
    `description`      TEXT         NULL,
    `transactionDate`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`point_history_id`),
    INDEX `IDX_POINTHISTORY_USERID_DATE` (`userId`, `transactionDate`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4) CONCERT
-- ============================================================
CREATE TABLE IF NOT EXISTS `CONCERT` (
    `concertId`  VARCHAR(255) NOT NULL,
    `name`       VARCHAR(255) NOT NULL,
    `artist`     VARCHAR(255) NOT NULL,
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`concertId`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 5) CONCERTSCHEDULE
-- ============================================================
CREATE TABLE IF NOT EXISTS `CONCERTSCHEDULE` (
    `concert_schedule_id` VARCHAR(255) NOT NULL,
    `concertId`           VARCHAR(255) NOT NULL,
    `availability`        VARCHAR(255) NOT NULL,
    `datetime`            DATETIME(6)  NOT NULL,
    `reservationStartAt`  DATETIME(6)  NOT NULL,
    `reservationEndAt`    DATETIME(6)  NOT NULL,
    `created_at`          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`concert_schedule_id`),
    INDEX `IDX_RESERVATION_AVAILABLE_PERIOD` (`reservationStartAt`, `reservationEndAt`),
    INDEX `IDX_CONCERTSCHEDULE_CONCERTID` (`concertId`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 6) SEAT
-- ============================================================
CREATE TABLE IF NOT EXISTS `SEAT` (
    `seatId`            VARCHAR(255) NOT NULL,
    `concertScheduleId` VARCHAR(255) NOT NULL,
    `number`            INT          NOT NULL,
    `price`             INT          NOT NULL,
    `status`            VARCHAR(255) NOT NULL,
    `created_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`seatId`),
    UNIQUE KEY `UK_SEAT_SCHEDULE_NUMBER` (`concertScheduleId`, `number`),
    INDEX `IDX_SEAT_CONCERT_SCHEDULE_ID` (`concertScheduleId`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 7) QUEUE
-- ============================================================
CREATE TABLE IF NOT EXISTS `QUEUE` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `concertScheduleId` VARCHAR(255)  NOT NULL,
    `userId`            VARCHAR(255)  NOT NULL,
    `status`            VARCHAR(255)  NOT NULL,
    `expiredAt`         DATETIME(6)   NULL,
    `created_at`        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `IDX_QUEUE_SCHEDULE_STATUS_CREATED` (`concertScheduleId`, `status`, `created_at`),
    INDEX `IDX_QUEUE_USER` (`userId`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 8) RESERVATION
-- ============================================================
CREATE TABLE IF NOT EXISTS `RESERVATION` (
    `reservation_id`    VARCHAR(255) NOT NULL,
    `userId`            VARCHAR(255) NOT NULL,
    `concertScheduleId` VARCHAR(255) NOT NULL,
    `seatId`            VARCHAR(255) NOT NULL,
    `price`             INT          NOT NULL,
    `status`            VARCHAR(255) NOT NULL,
    `expiredAt`         DATETIME(6)  NULL,
    `created_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`reservation_id`),
    INDEX `IDX_RESERVATION_USER` (`userId`),
    INDEX `IDX_RESERVATION_SCHEDULE` (`concertScheduleId`),
    INDEX `IDX_RESERVATION_SEAT` (`seatId`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 9) outbox
-- ============================================================
CREATE TABLE IF NOT EXISTS `outbox` (
    `id`              VARCHAR(255) NOT NULL,
    `payload`         JSON         NOT NULL,
    `status`          VARCHAR(255) NOT NULL,
    `topicIdentifier` VARCHAR(255) NOT NULL,
    `retryCount`      INT          NOT NULL DEFAULT 0,
    `created_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `IDX_OUTBOX_STATUS_CREATED` (`status`, `created_at`),
    INDEX `IDX_OUTBOX_TOPIC_STATUS` (`topicIdentifier`, `status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;