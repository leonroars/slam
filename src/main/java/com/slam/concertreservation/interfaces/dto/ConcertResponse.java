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

    /**
     * Create a ConcertResponse DTO from a Concert domain object.
     *
     * @param concert the domain Concert to map from
     * @return a ConcertResponse containing the concert's id (as a string), name, and artist
     */
    public static ConcertResponse from(Concert concert) {
        return ConcertResponse.builder()
                .id(String.valueOf(concert.getId()))
                .name(concert.getName())
                .artist(concert.getArtist())
                .build();
    }
}