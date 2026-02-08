package com.slam.concertreservation.domain.concert.model;

/**
 * 예약 가능 공연 일정 조회 시 반환되는 공연 일정 & 공연 정보 조합 모델
 * @param concertSchedule
 * @param concert
 */
public record ConcertScheduleWithConcert(
        ConcertSchedule concertSchedule,
        Concert concert
) {}
