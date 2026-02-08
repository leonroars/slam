package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.concert.model.Concert;
import com.slam.concertreservation.domain.concert.repository.ConcertRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.ConcertJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.ConcertJpaEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConcertRepositoryImpl implements ConcertRepository {

    private final ConcertJpaRepository concertJpaRepository;

    @Override
    public Concert save(Concert concert) {
        return concertJpaRepository.save(ConcertJpaEntity.fromDomain(concert))
                .toDomain();
    }

    @Override
    public Optional<Concert> findById(Long concertId) {
        return concertJpaRepository.findById(concertId)
                .map(ConcertJpaEntity::toDomain);
    }

    @Override
    public List<Concert> findAllById(List<Long> ids) {
        return concertJpaRepository.findAllById(ids)
                .stream()
                .map(ConcertJpaEntity::toDomain)
                .toList();
    }
}
