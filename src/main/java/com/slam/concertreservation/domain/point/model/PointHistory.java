package com.slam.concertreservation.domain.point.model;

import java.time.LocalDateTime;
import io.hypersistence.tsid.TSID;

/**
 * 사용자 포인트 잔액 변동 내역을 표현합니다.
 * 
 * @param pointHistoryId
 * @param userId
 * @param transactionType   - INIT, CHARGE, USE
 * @param transactionAmount
 * @param transactionDate
 */
public record PointHistory(
        Long pointHistoryId,
        Long userId,
        PointTransactionType transactionType,
        int transactionAmount,
        LocalDateTime transactionDate) {
    public static PointHistory create(Long userId,
            PointTransactionType transactionType,
            int transactionAmount) {
        return new PointHistory(
                TSID.fast().toLong(),
                userId,
                transactionType,
                transactionAmount,
                LocalDateTime.now());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PointHistory pointHistory) {
            return this.pointHistoryId().equals(pointHistory.pointHistoryId());
        }
        return false;
    }
}
