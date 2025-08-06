package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import com.slam.concertreservation.domain.reservation.repository.ReservationRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.ReservationJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.ReservationJpaEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {
    private final ReservationJpaRepository reservationJpaRepository;

    public Reservation save(Reservation reservation) {
        ReservationJpaEntity toSave = reservationJpaRepository.findById(reservation.getId())
                .map(existingEntity -> {
                    // 만약 기존재 예약 Entity 존재 시 업데이트 후 반환
                    existingEntity.updateFromDomain(reservation);
                    return existingEntity;
                })
                .orElseGet(() -> ReservationJpaEntity.fromDomain(reservation));

        // Finally, persist the JPA entity and return the domain model
        ReservationJpaEntity saved = reservationJpaRepository.save(toSave);
        return saved.toDomain();
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

    @Override
    public Optional<Reservation> findPendingReservationByUserId(String userId) {
        return reservationJpaRepository.findPendingReservationByUserId(userId)
                .map(ReservationJpaEntity::toDomain);
    }
}
