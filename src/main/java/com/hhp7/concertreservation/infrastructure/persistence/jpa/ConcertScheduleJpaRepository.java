package com.hhp7.concertreservation.infrastructure.persistence.jpa;

import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.ConcertScheduleJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConcertScheduleJpaRepository extends JpaRepository<ConcertScheduleJpaEntity, String> {

    @Query("select cs from ConcertScheduleJpaEntity cs "
            + "where cs.reservationStartAt <= :presentDateTime "
            + "AND cs.reservationEndAt >= :presentDateTime"
            + " AND cs.availability = 'AVAILABLE'")
    List<ConcertScheduleJpaEntity> findAllAvailable(@Param("presentDateTime") LocalDateTime presentDateTime);
}
