package com.slam.concertreservation.domain.reservation.event;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import java.time.LocalDateTime;

public record ReservationConfirmationEvent(
        String reservationId,
        String concertScheduleId,
        String userId,
        String seatId,
        int price,
        LocalDateTime updatedAt
) {

    public static ReservationConfirmationEvent fromDomain(Reservation reservation){
        return new ReservationConfirmationEvent(
                reservation.getId(),
                reservation.getConcertScheduleId(),
                reservation.getUserId(),
                reservation.getSeatId(),
                reservation.getPrice(),
                reservation.getUpdatedAt()
        );
    }
}
