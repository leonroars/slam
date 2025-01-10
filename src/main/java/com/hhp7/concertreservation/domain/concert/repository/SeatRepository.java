package com.hhp7.concertreservation.domain.concert.repository;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import java.util.List;
import java.util.Optional;

public interface SeatRepository {

    // 좌석 저장
    Seat save(Seat seat);

    // 좌석 조회
    Optional<Seat> findById(String seatId);

    // 특정 콘서트 일정의 예약 가능 좌석 목록 조회
    List<Seat> findAllAvailableByConcertScheduleId(String concertScheduleId);


}
