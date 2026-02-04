package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.concert.model.Seat;
import com.slam.concertreservation.domain.reservation.model.Reservation;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationResponse {
    private String id;
    private String userId;
    private String seatId;
    private int seatNumber;
    private String concertScheduleId;
    private int price;
    private String status;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;

    public static ReservationResponse from(Reservation reservation, Seat seat) {
        return ReservationResponse.builder()
                .id(String.valueOf(reservation.getId()))
                .userId(String.valueOf(reservation.getUserId()))
                .seatId(String.valueOf(reservation.getSeatId()))
                .seatNumber(seat.getNumber())
                .concertScheduleId(String.valueOf(reservation.getConcertScheduleId()))
                .price(reservation.getPrice())
                .status(reservation.getStatus().name())
                .expiredAt(reservation.getExpiredAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
