package com.hhp7.concertreservation.infrastructure.persistence.jpa.impl;

import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.repository.ReservationRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.ReservationJpaRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.ReservationJpaEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {
    private final ReservationJpaRepository reservationJpaRepository;

    public Reservation save(Reservation reservation){
        // 기존재 Entity 있는 경우 -> ReservationJpaEntity.updateFromDomain() 호출 이후 변경 내역 저장.
        if(reservation.getId() != null && !reservation.getId().isBlank()){
            ReservationJpaEntity existingEntity = reservationJpaRepository.findById(reservation.getId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Reservation ID입니다."));

            existingEntity = existingEntity.updateFromDomain(reservation);

            ReservationJpaEntity saved = reservationJpaRepository.save(existingEntity);
            return saved.toDomain();
        }
        // 새로운 Entity인 경우 -> ReservationJpaEntity.fromDomain() 호출 이후 저장.
        else {
            ReservationJpaEntity newEntity = ReservationJpaEntity.fromDomain(reservation);
            ReservationJpaEntity saved = reservationJpaRepository.save(newEntity);
            return saved.toDomain();
        }
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return reservationJpaRepository.findById(reservationId)
                .map(ReservationJpaEntity::toDomain);
    }

    @Override
    public List<Reservation> findByUserId(String userId) {
        return reservationJpaRepository.findByUserId(userId).stream()
                .map(ReservationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Reservation> findByConcertScheduleIdAndSeatId(String concertScheduleId, String seatId) {
        return reservationJpaRepository.findByConcertScheduleIdAndSeatId(concertScheduleId, seatId)
                .map(ReservationJpaEntity::toDomain);
    }

    @Override
    public Optional<Reservation> findByConcertScheduleIdAndUserId(String concertScheduleId, String userId) {
        return reservationJpaRepository.findByConcertScheduleIdAndUserId(concertScheduleId, userId)
                .map(ReservationJpaEntity::toDomain);
    }

    @Override
    public List<Reservation> findByConcertScheduleId(String concertScheduleId) {
        return reservationJpaRepository.findByConcertScheduleId(concertScheduleId).stream()
                .map(ReservationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Reservation> findAllByExpirationCriteria() {
        return reservationJpaRepository.findAllByExpirationCriteria().stream()
                .map(ReservationJpaEntity::toDomain)
                .toList();
    }
}
