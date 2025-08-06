package com.slam.concertreservation.domain.reservation.event;

import java.time.LocalDateTime;

public record ReservationConfirmationRollbackEvent(
        String reservationId,
        String concertScheduleId,
        String userId,
        String seatId,
        LocalDateTime failedAt
) {
    public static ReservationConfirmationRollbackEvent fromDomain(String reservationId, String concertScheduleId, String userId, String seatId){
        return new ReservationConfirmationRollbackEvent(
                reservationId,
                concertScheduleId,
                userId,
                seatId,
                LocalDateTime.now()
        );
    }
}
