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

    // 일정 상 예약 가능한 콘서트 목록 조회
    List<ConcertSchedule> findAllByAvailableDatetime(LocalDateTime dateTime);
}
