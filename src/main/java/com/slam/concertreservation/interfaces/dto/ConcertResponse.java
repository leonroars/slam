package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.concert.model.Concert;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConcertResponse {
    private String id;
    private String name;
    private String artist;

    public static ConcertResponse from(Concert concert) {
        return ConcertResponse.builder()
                .id(String.valueOf(concert.getId()))
                .name(concert.getName())
                .artist(concert.getArtist())
                .build();
    }
}
