package com.hhp7.concertreservation.domain.concert.model;

import com.hhp7.concertreservation.domain.point.model.Point;
import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Seat {
    private String id;
    private String concertScheduleId;
    private int number; // 50번까지만 존재하도록 제약 필요.
    private int price;
    private SeatStatus status;

    private Seat(){}

    /* Seat 인스턴스를 생성하는 것은 해당 좌석에 대한 예약이 진행되어 특정 사용자에게 '할당'되었음을 의미합니다. */
    public static Seat assign(String concertScheduleId, int number, int price, SeatStatus status){
        // 비즈니스 정책 검증
        if(number < 1 || number > 50){
            throw new BusinessRuleViolationException("존재할 수 없는 좌석 번호입니다.");
        }

        Seat seat = new Seat();
        seat.id = UUID.randomUUID().toString();
        seat.concertScheduleId = concertScheduleId;
        seat.number = number;
        seat.price = price;
        seat.status = status;

        return seat;
    }

    public static Seat assign(String id, String concertScheduleId, int number, int price, SeatStatus status){
        // 비즈니스 정책 검증
        if(number < 1 || number > 50){
            throw new BusinessRuleViolationException("존재할 수 없는 좌석 번호입니다.");
        }
        Seat seat = new Seat();
        seat.id = id;
        seat.concertScheduleId = concertScheduleId;
        seat.number = number;
        seat.price = price;
        seat.status = status;

        return seat;
    }

    // 도메인 로직 : 좌석 상태 변경
    public void updateStatus(SeatStatus status){
        this.status = status;
    }

    public boolean isAvailable(){
        return this.status == SeatStatus.AVAILABLE;
    }

}
