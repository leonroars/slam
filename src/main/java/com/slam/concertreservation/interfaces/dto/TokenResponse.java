package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.queue.model.Token;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String id;
    private String userId;
    private String concertScheduleId;
    private String status;
    private LocalDateTime expiredAt;

    public static TokenResponse from(Token token) {
        return TokenResponse.builder()
                .id(token.getId()) // Token ID is already String
                .userId(String.valueOf(token.getUserId()))
                .concertScheduleId(String.valueOf(token.getConcertScheduleId()))
                .status(token.getStatus().name())
                .expiredAt(token.getExpiredAt())
                .build();
    }
}
