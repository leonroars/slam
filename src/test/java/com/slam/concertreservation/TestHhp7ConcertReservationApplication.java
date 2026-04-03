package com.slam.concertreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
public class TestHhp7ConcertReservationApplication {

    public static void main(String[] args) {
        SpringApplication.from(Hhp7ConcertReservationApplication::main).with(TestcontainersConfiguration.class)
                .run(args);
    }

}
