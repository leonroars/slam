package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.point.model.UserPointBalance;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPointBalanceResponse {
    private String id;
    private String userId;
    private int amount;

    public static UserPointBalanceResponse from(UserPointBalance balance) {
        return UserPointBalanceResponse.builder()
                .id(String.valueOf(balance.getId()))
                .userId(String.valueOf(balance.getUserId()))
                .amount(balance.getBalance().getAmount())
                .build();
    }
}
