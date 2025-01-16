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

    @Override
    public Reservation save(Reservation reservation) {
        return reservationJpaRepository.save(ReservationJpaEntity.fromDomain(reservation))
                .toDomain();
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
