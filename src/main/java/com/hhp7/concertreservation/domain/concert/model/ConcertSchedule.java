package com.hhp7.concertreservation.domain.concert.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ConcertSchedule {
    private String id;
    private String concertId;
    private LocalDateTime dateTime;
    private LocalDateTime reservationStartAt;
    private LocalDateTime reservationEndAt;

    private ConcertSchedule() {}

    // ID 포함 생성자
    public static ConcertSchedule create(String id, String concertId, LocalDateTime dateTime, LocalDateTime reservationStartAt, LocalDateTime reservationEndAt){

        if(reservationStartAt.isAfter(reservationEndAt)){
            throw new BusinessRuleViolationException("예약 시작 일자는 예약 종료 일자보다 늦을 수 없습니다.");
        }
        ConcertSchedule concertSchedule = new ConcertSchedule();
        concertSchedule.id = id;
        concertSchedule.concertId = concertId;
        concertSchedule.dateTime = dateTime;
        concertSchedule.reservationStartAt = reservationStartAt;
        concertSchedule.reservationEndAt = reservationEndAt;

        return concertSchedule;
    }

    // ID 미포함 생성자
    public static ConcertSchedule create(String concertId, LocalDateTime dateTime, LocalDateTime reservationStartAt, LocalDateTime reservationEndAt){
        if(reservationStartAt.isAfter(reservationEndAt)){
            throw new BusinessRuleViolationException("예약 시작 일자는 예약 종료 일자보다 늦을 수 없습니다.");
        }

        ConcertSchedule concertSchedule = new ConcertSchedule();
        concertSchedule.id = UUID.randomUUID().toString();
        concertSchedule.concertId = concertId;
        concertSchedule.dateTime = dateTime;
        concertSchedule.reservationStartAt = reservationStartAt;
        concertSchedule.reservationEndAt = reservationEndAt;

        return concertSchedule;
    }



}
