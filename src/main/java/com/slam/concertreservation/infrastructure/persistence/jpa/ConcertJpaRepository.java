package com.slam.concertreservation.infrastructure.persistence.jpa;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.ConcertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertJpaRepository extends JpaRepository<ConcertJpaEntity, Long> {
}
