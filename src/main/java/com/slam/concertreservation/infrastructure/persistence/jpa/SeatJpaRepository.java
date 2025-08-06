package com.slam.concertreservation.infrastructure.persistence.jpa;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.SeatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, String> {

    List<SeatJpaEntity> findAllByConcertScheduleId(String concertScheduleId);

    @Query("SELECT s FROM SeatJpaEntity s WHERE s.concertScheduleId = :concertScheduleId AND s.status = 'AVAILABLE'")
    List<SeatJpaEntity> findAllAvailableSeatsByConcertScheduleId(String concertScheduleId);

    @Query("SELECT COUNT(s) FROM SeatJpaEntity s WHERE s.concertScheduleId = :concertScheduleId AND s.status = 'UNAVAILABLE'")
    int findOccupiedSeatsCount(@Param("concertScheduleId") String concertScheduleId);
}
