package com.slam.concertreservation.infrastructure.persistence.jpa;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.CompensationTxLogJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompensationTxLogJpaRepository extends JpaRepository<CompensationTxLogJpaEntity, Long> {

    List<CompensationTxLogJpaEntity> findAllByStatus(String status);

    List<CompensationTxLogJpaEntity> findAllByRetryCountGreaterThanEqual(int maxRetryCount);

    List<CompensationTxLogJpaEntity> findAllByRetryCountLessThan(int maxRetryCount);

    List<CompensationTxLogJpaEntity> findAllByUserId(Long userId);

    List<CompensationTxLogJpaEntity> findAllByReservationId(Long reservationId);

    List<CompensationTxLogJpaEntity> findAllByUserIdAndReservationId(Long userId, Long reservationId);

    List<CompensationTxLogJpaEntity> findAllByCreatedAtBetween(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime);
}
