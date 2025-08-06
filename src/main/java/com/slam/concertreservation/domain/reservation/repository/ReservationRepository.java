package com.slam.concertreservation.domain.reservation.repository;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    // 예약 저장.
    Reservation save(Reservation reservation);

    // 예약 ID로 예약 조회
    Optional<Reservation> findById(String reservationId);

    // 사용자 ID로 예약 조회
    List<Reservation> findByUserId(String userId);

    // 공연 일정 ID와 좌석 ID로 예약 조회. 단 건 조회인 대신 Service.register() 시 중복 여부 검증을 수행한다.
    Optional<Reservation> findByConcertScheduleIdAndSeatId(String concertScheduleId, String seatId);

    // 공연 일정 ID와 사용자 ID로 예약 조회.
    Optional<Reservation> findByConcertScheduleIdAndUserId(String concertScheduleId, String userId);

    // 공연 일정 ID로 예약 조회
    List<Reservation> findByConcertScheduleId(String concertScheduleId);

    // 만료 대상인 예약 전체 조회.
    List<Reservation> findAllByExpirationCriteria();

    // 특정 유저의 가예약 조회.
    Optional<Reservation> findPendingReservationByUserId(String userId);


}
