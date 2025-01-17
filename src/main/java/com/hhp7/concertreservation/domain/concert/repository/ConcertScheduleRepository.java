package com.hhp7.concertreservation.domain.concert.repository;


import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConcertScheduleRepository {

    // 콘서트 일정 생성
    ConcertSchedule save(ConcertSchedule concertSchedule);

    // 콘서트 일정 조회
    Optional<ConcertSchedule> findById(String concertScheduleId);

    // 예약 가능한 콘서트 목록 조회(일정 상 예약 가능함과 동시에 예약 가능 좌석도 존재하는 공연 일정 목록 조회)
    List<ConcertSchedule> findAllAvailable(LocalDateTime presentDateTime);
}
