package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.concert.model.Seat;
import com.slam.concertreservation.domain.concert.repository.SeatRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.SeatJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.SeatJpaEntity;
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
        return seatJpaRepository.findById(seat.getId())
                .map(seatJpaEntity -> {
                    seatJpaEntity.updateFromDomain(seat);
                    return seatJpaRepository.save(seatJpaEntity).toDomain();
                })
                .orElseGet(() -> seatJpaRepository.save(SeatJpaEntity.fromDomain(seat)).toDomain());
    }

    @Override
    public List<Seat> saveAll(List<Seat> seats) {
        return seatJpaRepository.saveAll(SeatJpaEntity.createSeatEntitiesFromDomain(seats))
                .stream()
                .map(SeatJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Seat> findById(Long seatId) {
        return seatJpaRepository.findById(seatId)
                .map(SeatJpaEntity::toDomain);
    }

    @Override
    public List<Seat> findAllByConcertScheduleId(Long concertScheduleId) {
        return seatJpaRepository.findAllByConcertScheduleId(concertScheduleId)
                .stream()
                .map(SeatJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Seat> findAllAvailableSeatsByConcertScheduleId(Long concertScheduleId) {
        return seatJpaRepository.findAllAvailableSeatsByConcertScheduleId(concertScheduleId)
                .stream()
                .map(SeatJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int findOccupiedSeatsCount(Long concertScheduleId) {
        return seatJpaRepository.findOccupiedSeatsCount(concertScheduleId);
    }
}
