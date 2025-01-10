package com.hhp7.concertreservation.domain.point.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record PointHistory(
        String pointHistoryId,
        String userId,
        PointTransactionType transactionType,
        int transactionAmount,
        LocalDateTime transactionDate
) {
    public static PointHistory create(String userId,
                                      PointTransactionType transactionType,
                                      int transactionAmount){
        return new PointHistory(
                String.valueOf(UUID.randomUUID()),
                userId,
                transactionType,
                transactionAmount,
                LocalDateTime.now());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof PointHistory pointHistory){
            return this.pointHistoryId().equals(pointHistory.pointHistoryId());
        }
        return false;
    }
}
