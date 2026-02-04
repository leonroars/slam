package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.point.model.PointHistory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointHistoryResponse {
    private String id;
    private String userId;
    private String transactionType;
    private int amount;
    private LocalDateTime transactionDate;

    public static PointHistoryResponse from(PointHistory history) {
        return PointHistoryResponse.builder()
                .id(String.valueOf(history.pointHistoryId()))
                .userId(String.valueOf(history.userId()))
                .transactionType(history.transactionType().name())
                .amount(history.transactionAmount())
                .transactionDate(history.transactionDate())
                .build();
    }
}
