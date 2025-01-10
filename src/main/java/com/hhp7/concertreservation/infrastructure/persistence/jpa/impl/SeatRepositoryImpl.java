package com.hhp7.concertreservation.infrastructure.persistence.jpa.impl;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.repository.SeatRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.SeatJpaRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.SeatJpaEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryImpl implements SeatRepository {

    private final SeatJpaRepository seatJpaRepository;

    @Override
    public Seat save(Seat seat) {
        return seatJpaRepository.save(SeatJpaEntity.fromDomainModel(seat))
                .toDomainModel();
    }

    @Override
    public Optional<Seat> findById(String seatId) {
        return seatJpaRepository.findBySeatId(seatId)
                .map(SeatJpaEntity::toDomainModel);
    }

    @Override
    public List<Seat> findAllAvailableByConcertScheduleId(String concertScheduleId) {
        return seatJpaRepository.findAllByConcertScheduleId(concertScheduleId)
                .stream()
                .map(SeatJpaEntity::toDomainModel)
                .toList();
    }
}

