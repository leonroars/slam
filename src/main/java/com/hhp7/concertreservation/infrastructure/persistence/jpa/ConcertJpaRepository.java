package com.hhp7.concertreservation.infrastructure.persistence.jpa;

import com.hhp7.concertreservation.domain.concert.model.Concert;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.ConcertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertJpaRepository extends JpaRepository<ConcertJpaEntity, String> {
}
