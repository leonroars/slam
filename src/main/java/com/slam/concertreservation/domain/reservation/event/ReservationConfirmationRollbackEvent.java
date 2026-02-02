package com.slam.concertreservation.domain.reservation.event;

import java.time.LocalDateTime;

public record ReservationConfirmationRollbackEvent(
        Long reservationId,
        Long concertScheduleId,
        Long userId,
        Long seatId,
        LocalDateTime failedAt) {
    public static ReservationConfirmationRollbackEvent fromDomain(Long reservationId, Long concertScheduleId,
            Long userId, Long seatId) {
        return new ReservationConfirmationRollbackEvent(
                reservationId,
                concertScheduleId,
                userId,
                seatId,
                LocalDateTime.now());
    }
}
