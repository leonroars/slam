package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.concert.model.Concert;
import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConcertScheduleResponse {
    private String id;
    private String concertId;
    private String concertTitle;
    private String concertArtist;
    private LocalDateTime dateTime;
    private LocalDateTime reservationStartAt;
    private LocalDateTime reservationEndAt;
    private String status;

    /**
     * Create a ConcertScheduleResponse DTO from a ConcertSchedule and its associated Concert.
     *
     * @param schedule the schedule containing date/time and reservation/availability details
     * @param concert  the concert providing title and artist information
     * @return a ConcertScheduleResponse populated from the provided schedule and concert
     */
    public static ConcertScheduleResponse from(ConcertSchedule schedule, Concert concert) {
        return ConcertScheduleResponse.builder()
                .id(String.valueOf(schedule.getId()))
                .concertId(String.valueOf(schedule.getConcertId()))
                .concertTitle(concert.getName())
                .concertArtist(concert.getArtist())
                .dateTime(schedule.getDateTime())
                .reservationStartAt(schedule.getReservationStartAt())
                .reservationEndAt(schedule.getReservationEndAt())
                .status(schedule.getAvailability().name())
                .build();
    }
}