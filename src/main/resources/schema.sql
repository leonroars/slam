CREATE TABLE `USER` (
  `user_id` VARCHAR(16) PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `created_at` DATETIME DEFAULT (CURRENT_TIMESTAMP),
  `updated_at` DATETIME DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `POINTBALANCE` (
  `point_balance_id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` VARCHAR(16) NOT NULL,
  `balance` INT NOT NULL
);

CREATE TABLE `POINTHISTORY` (
  `point_history_id` VARCHAR(16) PRIMARY KEY,
  `user_id` VARCHAR(16) NOT NULL,
  `type` ENUM ('charge', 'use') NOT NULL,
  `amount` INT NOT NULL,
  `created_at` DATETIME DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `CONCERT` (
  `concert_id` VARCHAR(16) PRIMARY KEY,
  `artist` VARCHAR(200) NOT NULL,
  `name` VARCHAR(200) NOT NULL
);

CREATE TABLE `CONCERTSCHEDULE` (
  `concert_schedule_id` VARCHAR(16) PRIMARY KEY,
  `concert_id` VARCHAR(16) NOT NULL,
#   `available_seats_count` INT DEFAULT (50),
  `availability` ENUM ('AVAILABLE', 'SOLDOUT') DEFAULT 'AVAILABLE',
  `datetime` DATETIME NOT NULL,
  `reservation_start_at` DATETIME NOT NULL,
  `reservation_end_at` DATETIME NOT NULL,
  `created_at` DATETIME DEFAULT (CURRENT_TIMESTAMP),
  `updated_at` DATETIME DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `SEAT` (
  `seat_id` VARCHAR(16) PRIMARY KEY,
  `concert_schedule_id` VARCHAR(16) NOT NULL,
  `number` VARCHAR(10) NOT NULL,
  `price` INT NOT NULL,
  `status` ENUM ('AVAILABLE', 'UNAVAILABLE') DEFAULT 'AVAILABLE',
  `created_at` DATETIME DEFAULT (CURRENT_TIMESTAMP),
  `updated_at` DATETIME DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `QUEUE` (
  `token_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` VARCHAR(16) NOT NULL,
  `token_status` ENUM ('ACTIVE', 'WAIT', 'EXPIRED') DEFAULT 'WAIT',
  `concert_schedule_id` VARCHAR(16),
  `created_at` DATETIME DEFAULT (CURRENT_TIMESTAMP),
  `expires_at` DATETIME NOT NULL
);

CREATE TABLE `RESERVATION` (
  `reservation_id` VARCHAR(16) PRIMARY KEY,
  `user_id` VARCHAR(16) NOT NULL,
  `seat_id` VARCHAR(16) NOT NULL,
  `concert_schedule_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT (CURRENT_TIMESTAMP),
  `expire_at` DATETIME NOT NULL,
  `reservation_status` ENUM ('PAID', 'BOOKED', 'EXPIRED', 'CANCELLED') DEFAULT 'BOOKED'
);

CREATE UNIQUE INDEX `SEAT_index_0` ON `SEAT` (`concert_schedule_id`, `number`);

CREATE UNIQUE INDEX `RESERVATION_index_1` ON `RESERVATION` (`seat_id`, `concert_schedule_id`);
