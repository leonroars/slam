package com.slam.concertreservation.domain.reservation.event;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import java.time.LocalDateTime;

/**
 * 예약 만료 이벤트
 * @param reservationId
 * @param concertScheduleId
 * @param userId
 * @param seatId
 * @param expiredAt
 */
public record ReservationExpirationEvent(
        String reservationId,
        String concertScheduleId,
        String userId,
        String seatId,
        Integer price,
        LocalDateTime expiredAt
) {

    public static ReservationConfirmationEvent fromDomain(Reservation reservation){
        return new ReservationConfirmationEvent(
                reservation.getId(),
                reservation.getConcertScheduleId(),
                reservation.getUserId(),
                reservation.getSeatId(),
                reservation.getPrice(),
                reservation.getExpiredAt()
        );
    }
}
