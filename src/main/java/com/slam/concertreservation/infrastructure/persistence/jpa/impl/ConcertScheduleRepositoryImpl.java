package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.ConcertScheduleJpaRepository;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.ConcertScheduleJpaEntity;
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
        return concertScheduleJpaRepository.save(ConcertScheduleJpaEntity.fromDomain(concertSchedule))
                .toDomain();
    }

    @Override
    public Optional<ConcertSchedule> findById(String concertScheduleId) {
        return concertScheduleJpaRepository.findById(concertScheduleId)
                .map(ConcertScheduleJpaEntity::toDomain);
    }

    @Override
    public List<ConcertSchedule> findAllAvailable(LocalDateTime presentDateTime) {
        return concertScheduleJpaRepository.findAllAvailable(presentDateTime)
                .stream()
                .map(ConcertScheduleJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<ConcertSchedule> findAllOngoingConcertSchedules(LocalDateTime presentDateTime) {
        return concertScheduleJpaRepository.findAllOnGoing(presentDateTime)
                .stream()
                .map(ConcertScheduleJpaEntity::toDomain)
                .toList();
    }
}
