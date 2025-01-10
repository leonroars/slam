package com.hhp7.concertreservation.infrastructure.persistence.jpa;

import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.SeatJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, String> {

    Optional<SeatJpaEntity> findBySeatId(String seatId);

    List<SeatJpaEntity> findAllByConcertScheduleId(String concertScheduleId);
}
