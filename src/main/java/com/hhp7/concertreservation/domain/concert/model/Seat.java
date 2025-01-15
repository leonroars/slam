package com.hhp7.concertreservation.domain.concert.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Seat {
    private String id;
    private String concertScheduleId;
    private int number; // 50번까지만 존재하도록 제약 필요.
    private int price;
    private SeatStatus status;

    public static final int MAX_SEAT_NUMBER = 50;
    public static final int MIN_SEAT_NUMBER = 1;

    private Seat(){}

    /**
     * 좌석 정보 생성하는 정적 팩토리 메서드
     * @param concertScheduleId
     * @param number
     * @param price
     * @param status
     * @return
     */
    public static Seat create(String concertScheduleId, int number, int price, SeatStatus status){
        // 비즈니스 정책 검증
        if(number < MIN_SEAT_NUMBER || number > MAX_SEAT_NUMBER){
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

    /**
     * SEAT ID 를 포함한 정적 팩토리 메소드.
     * @param seatId
     * @param concertScheduleId
     * @param number
     * @param price
     * @param status
     * @return
     */
    public static Seat create(String seatId, String concertScheduleId, int number, int price, SeatStatus status){
        Seat seat = new Seat();
        seat.id = seatId;
        seat.concertScheduleId = concertScheduleId;
        seat.number = number;
        seat.price = price;
        seat.status = status;
        return seat;
    }

    /**
     * 새롭게 생성된 공연 일정을 위해 해당 공연에 할당 가능한 최대 좌석 수만큼 생성하기.
     * @param concertScheduleId
     * @param price
     * @param numOfSeats
     * @return
     */
    public static List<Seat> createSeatsForNewConcertSchedule(String concertScheduleId, int price, int numOfSeats){
        List<Seat> seats = new ArrayList<>();
        for(int i = MIN_SEAT_NUMBER; i < numOfSeats + 1; i++){
            seats.add(create(concertScheduleId, i, price, SeatStatus.AVAILABLE));
        }
        return seats;
    }

    // 도메인 로직 : 좌석 상태를 예약 불가 상태로 변경한다.
    public void makeUnavailable(){
        if(this.status == SeatStatus.UNAVAILABLE){
            throw new BusinessRuleViolationException("이미 선점되었거나 이용 불가한 좌석입니다.");
        }
        this.status = SeatStatus.UNAVAILABLE;
    }

    // 도메인 로직 : 좌석 상태를 예약 가능하도록 변경한다.
    public void makeAvailable(){
        if(this.status == SeatStatus.AVAILABLE){
            throw new BusinessRuleViolationException("이미 예약 가능 상태인 좌석입니다.");
        }
        this.status = SeatStatus.AVAILABLE;
    }

    // 해당 좌석 예약 가능한지
    public boolean isAvailable(){
        return this.status == SeatStatus.AVAILABLE;
    }
}
