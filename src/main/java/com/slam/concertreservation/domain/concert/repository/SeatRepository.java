package com.slam.concertreservation.domain.concert.repository;

import com.slam.concertreservation.domain.concert.model.Seat;
import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    // 좌석 저장
    Seat save(Seat seat);

    // 최초 생성 좌석 전체 저장
    List<Seat> saveAll(List<Seat> seats);

    // 좌석 조회
    Optional<Seat> findById(String seatId);

    // 특정 공연 일정의 전체 좌석 목록 조회
    List<Seat> findAllByConcertScheduleId(String concertScheduleId);

    // 특정 공연 일정의 예약 가능 좌석 목록 조회
    List<Seat> findAllAvailableSeatsByConcertScheduleId(String concertScheduleId);

    // 특정 공연 일정의 선점 좌석 수 집계
    int findOccupiedSeatsCount(String concertScheduleId);
}
