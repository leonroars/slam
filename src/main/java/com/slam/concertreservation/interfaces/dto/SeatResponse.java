package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.concert.model.Seat;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatResponse {
    private String id;
    private String concertScheduleId;
    private int seatNumber;
    private int price;
    private String status;

    public static SeatResponse from(Seat seat) {
        return SeatResponse.builder()
                .id(String.valueOf(seat.getId()))
                .concertScheduleId(String.valueOf(seat.getConcertScheduleId()))
                .seatNumber(seat.getNumber())
                .price(seat.getPrice())
                .status(seat.getStatus().name())
                .build();
    }
}
