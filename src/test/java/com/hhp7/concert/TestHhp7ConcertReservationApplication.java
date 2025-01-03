package com.hhp7.concert;

import org.springframework.boot.SpringApplication;

public class TestHhp7ConcertReservationApplication {

    public static void main(String[] args) {
        SpringApplication.from(Hhp7ConcertReservationApplication::main).with(TestcontainersConfiguration.class)
                .run(args);
    }

}
