package com.hhp7.concertreservation.infrastructure.persistence.jpa.impl;

import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.ConcertScheduleJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConcertScheduleRepositoryImpl implements ConcertScheduleRepository {
    private final ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Override
    public ConcertSchedule save(ConcertSchedule concertSchedule) {
        return concertScheduleJpaRepository.save(ConcertScheduleJpaEntity.fromDomainModel(concertSchedule))
                .toDomainModel();
    }

    @Override
    public Optional<ConcertSchedule> findById(String concertScheduleId) {
        return concertScheduleJpaRepository.findById(concertScheduleId)
                .map(ConcertScheduleJpaEntity::toDomainModel);
    }

    @Override
    public List<ConcertSchedule> findAllByAvailableDatetime(LocalDateTime dateTime) {
        return concertScheduleJpaRepository.findAllAvailableConcertSchedule(dateTime)
                .stream()
                .map(ConcertScheduleJpaEntity::toDomainModel)
                .toList();
    }
}

