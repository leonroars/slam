package com.slam.concertreservation.domain.reservation.event;

import java.time.LocalDateTime;

public record ReservationConfirmationRollbackEvent(
        Long reservationId,
        String concertScheduleId,
        Long userId,
        String seatId,
        LocalDateTime failedAt) {
    public static ReservationConfirmationRollbackEvent fromDomain(Long reservationId, String concertScheduleId,
            Long userId, String seatId) {
        return new ReservationConfirmationRollbackEvent(
                reservationId,
                concertScheduleId,
                userId,
                seatId,
                LocalDateTime.now());
    }
}
