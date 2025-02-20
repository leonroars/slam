package com.hhp7.concertreservation.infrastructure.persistence.jpa;

import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.ReservationJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, String> {

    List<ReservationJpaEntity> findByUserId(String userId);

    // 공연 일정 ID와 좌석 ID로 예약 조회. 단 건 조회인 대신 Service.register() 시 중복 여부 검증을 수행한다.
    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.concertScheduleId = :concertScheduleId AND r.seatId = :seatId")
    Optional<ReservationJpaEntity> findByConcertScheduleIdAndSeatId(@Param("concertScheduleId") String concertScheduleId, @Param("seatId") String seatId);

    // 공연 일정 ID와 사용자 ID로 예약 조회.
    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.concertScheduleId = :concertScheduleId AND r.userId = :userId")
    Optional<ReservationJpaEntity> findByConcertScheduleIdAndUserId(@Param("concertScheduleId") String concertScheduleId, @Param("userId") String userId);


    // 공연 일정 ID로 예약 조회
    List<ReservationJpaEntity> findByConcertScheduleId(String concertScheduleId);

    // 만료 대상 예약 목록 조회
    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.status = 'BOOKED' AND r.expiredAt < CURRENT_TIMESTAMP")
    List<ReservationJpaEntity> findAllByExpirationCriteria();

    // 특정 유저의 가예약 조회.
    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.userId = :userId AND r.status = 'BOOKED'")
    Optional<ReservationJpaEntity> findPendingReservationByUserId(@Param("userId") String userId);
}
