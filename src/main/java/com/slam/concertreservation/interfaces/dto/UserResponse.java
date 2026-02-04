package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private String id;
    private String name;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .name(user.getName())
                .build();
    }
}
