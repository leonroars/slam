package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private String id;
    private String name;

    /**
     * Create a UserResponse DTO from a domain User.
     *
     * @param user the domain User to convert
     * @return the UserResponse with `id` set to the string form of the user's id and `name` copied from the user
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .name(user.getName())
                .build();
    }
}