package com.hhp7.concertreservation.infrastructure.persistence.jpa;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.SeatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, String> {

    List<SeatJpaEntity> findAllByConcertScheduleId(String concertScheduleId);
}
