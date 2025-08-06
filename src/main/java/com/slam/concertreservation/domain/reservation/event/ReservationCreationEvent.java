package com.slam.concertreservation.domain.reservation.event;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import java.time.LocalDateTime;

public record ReservationCreationEvent(
        String reservationId,
        String concertScheduleId,
        String userId,
        String seatId,
        LocalDateTime createdAt
) {
    public static ReservationCreationEvent fromDomain(Reservation reservation){
        return new ReservationCreationEvent(
                reservation.getId(),
                reservation.getConcertScheduleId(),
                reservation.getUserId(),
                reservation.getSeatId(),
                reservation.getCreatedAt()
        );
    }
}
