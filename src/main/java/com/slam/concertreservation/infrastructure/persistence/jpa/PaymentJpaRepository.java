package com.slam.concertreservation.infrastructure.persistence.jpa;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.PaymentJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {

    List<PaymentJpaEntity> findAllByReservationId(Long reservationId);

    Page<PaymentJpaEntity> findAllByUserId(Long userId, Pageable pageable);

    List<PaymentJpaEntity> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    List<PaymentJpaEntity> findAllByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}
