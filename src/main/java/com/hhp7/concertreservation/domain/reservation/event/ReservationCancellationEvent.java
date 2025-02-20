package com.hhp7.concertreservation.domain.reservation.event;

import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import java.time.LocalDateTime;

public record ReservationCancellationEvent(
        String reservationId,
        String concertScheduleId,
        String userId,
        String seatId,
        Integer price,
        LocalDateTime cancelledAt
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
