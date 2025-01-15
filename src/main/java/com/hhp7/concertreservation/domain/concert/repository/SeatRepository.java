package com.hhp7.concertreservation.domain.concert.repository;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    // 좌석 저장
    Seat save(Seat seat);

    // 최초 생성 좌석 전체 저장
    List<Seat> saveAll(List<Seat> seats);

    // 좌석 조회
    Optional<Seat> findById(String seatId);

    // 특정 공연 일정의 예약 가능 좌석 목록 조회
    List<Seat> findAllByConcertScheduleId(String concertScheduleId);

}
