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

    /**
     * Creates a ReservationResponse DTO populated from the given Reservation and Seat.
     *
     * @param reservation the reservation entity to extract reservation fields from
     * @param seat the seat entity to extract seat-specific fields such as seatNumber
     * @return a ReservationResponse containing mapped values from the provided reservation and seat
     */
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