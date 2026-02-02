package com.slam.concertreservation.domain.reservation.event;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import java.time.LocalDateTime;

public record ReservationCancellationEvent(
        Long reservationId,
        String concertScheduleId,
        Long userId,
        String seatId,
        Integer price,
        LocalDateTime cancelledAt) {

    public static ReservationCancellationEvent fromDomain(Reservation reservation) {
        return new ReservationCancellationEvent(
                reservation.getId(),
                reservation.getConcertScheduleId(),
                reservation.getUserId(),
                reservation.getSeatId(),
                reservation.getPrice(),
                reservation.getUpdatedAt());
    }
}
