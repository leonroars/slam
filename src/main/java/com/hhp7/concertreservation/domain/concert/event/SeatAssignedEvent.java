package com.hhp7.concertreservation.domain.concert.event;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import java.time.LocalDateTime;

public record SeatAssignedEvent(
        String concertScheduleId,
        String seatId,
        String userId,
        Integer price,
        LocalDateTime assignedAt
) {

    public static SeatAssignedEvent fromDomain(Seat assignedSeat, String userId){
        return new SeatAssignedEvent(
                assignedSeat.getConcertScheduleId(),
                assignedSeat.getId(),
                userId,
                assignedSeat.getPrice(),
                LocalDateTime.now()
        );
    }
}
