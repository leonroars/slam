package com.hhp7.concertreservation.infrastructure.persistence.jpa;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.SeatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, String> {

    List<SeatJpaEntity> findAllByConcertScheduleId(String concertScheduleId);

    @Query("SELECT s FROM SeatJpaEntity s WHERE s.concertScheduleId = :concertScheduleId AND s.status = 'AVAILABLE'")
    List<SeatJpaEntity> findAllAvailableSeatsByConcertScheduleId(String concertScheduleId);
}
