package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import com.slam.concertreservation.domain.payment.model.Payment;
import com.slam.concertreservation.domain.payment.repository.PaymentRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.PaymentJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.PaymentJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(PaymentJpaEntity.fromDomain(payment))
                .toDomain();
    }

    @Override
    public Optional<Payment> findById(Long paymentId) {
        return paymentJpaRepository.findById(paymentId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public List<Payment> findAllByReservationId(Long reservationId) {
        return paymentJpaRepository.findAllByReservationId(reservationId).stream()
                .map(PaymentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Page<Payment> findAllByUserId(Long userId, Pageable pageable) {
        return paymentJpaRepository.findAllByUserId(userId, pageable)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public List<Payment> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDateTime,
                                                            LocalDateTime endDateTime) {
        return paymentJpaRepository.findAllByUserIdAndCreatedAtBetween(userId, startDateTime, endDateTime).stream()
                .map(PaymentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findAllByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return paymentJpaRepository.findAllByCreatedAtBetween(startDateTime, endDateTime).stream()
                .map(PaymentJpaEntity::toDomain)
                .toList();
    }
}
