package com.hhp7.concertreservation.infrastructure.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    ERROR // 5회 이상 전송 시도 후 실패 시 상태 변경
}
