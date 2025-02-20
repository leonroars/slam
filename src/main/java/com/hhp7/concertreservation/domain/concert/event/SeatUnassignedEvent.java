package com.hhp7.concertreservation.domain.concert.event;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import java.time.LocalDateTime;

/**
 * 좌석이 해제되었을 때 발생하는 이벤트
 * @param concertScheduleId
 * @param seatId
 * @param userId
 * @param price
 * @param unassignedAt
 */
public record SeatUnassignedEvent(
        String concertScheduleId,
        String seatId,
        Integer price,
        LocalDateTime unassignedAt
) {

    public static SeatUnassignedEvent fromDomain(Seat unasignedSeat){
        return new SeatUnassignedEvent(
                unasignedSeat.getConcertScheduleId(),
                unasignedSeat.getId(),
                unasignedSeat.getPrice(),
                LocalDateTime.now()
        );
    }
}
