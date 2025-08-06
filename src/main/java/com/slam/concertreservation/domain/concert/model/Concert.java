package com.slam.concertreservation.domain.concert.model;

import java.util.UUID;
import lombok.Getter;

@Getter
public class Concert {
    private String id;
    private String name;
    private String artist;

    private Concert(){}

    public static Concert create(String id, String name, String artist) {
        Concert concert = new Concert();
        concert.id = id;
        concert.name = name;
        concert.artist = artist;
        return concert;
    }

    public static Concert create(String name, String artist) {
        Concert concert = new Concert();
        concert.id = UUID.randomUUID().toString();
        concert.name = name;
        concert.artist = artist;
        return concert;
    }
}