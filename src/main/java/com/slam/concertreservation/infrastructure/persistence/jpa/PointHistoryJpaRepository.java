package com.slam.concertreservation.infrastructure.persistence.jpa;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.PointHistoryJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryJpaEntity, Long> {

    List<PointHistoryJpaEntity> findByUserId(Long userId);
}
