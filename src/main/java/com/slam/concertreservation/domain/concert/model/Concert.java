package com.slam.concertreservation.domain.concert.model;

import io.hypersistence.tsid.TSID;
import lombok.Getter;

@Getter
public class Concert {
    private Long id;
    private String name;
    private String artist;

    private Concert() {
    }

    public static Concert create(Long id, String name, String artist) {
        Concert concert = new Concert();
        concert.id = id;
        concert.name = name;
        concert.artist = artist;
        return concert;
    }

    public static Concert create(String name, String artist) {
        Concert concert = new Concert();
        concert.id = TSID.fast().toLong();
        concert.name = name;
        concert.artist = artist;
        return concert;
    }
}