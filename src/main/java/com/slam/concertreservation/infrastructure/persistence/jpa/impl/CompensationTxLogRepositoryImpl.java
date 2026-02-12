package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.payment.model.CompensationTxLog;
import com.slam.concertreservation.domain.payment.model.CompensationTxStatus;
import com.slam.concertreservation.domain.payment.repository.CompensationTxLogRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.CompensationTxLogJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.CompensationTxLogJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompensationTxLogRepositoryImpl implements CompensationTxLogRepository {

    private final CompensationTxLogJpaRepository compensationTxLogJpaRepository;

    @Override
    public CompensationTxLog save(CompensationTxLog compensationTxLog) {
        return compensationTxLogJpaRepository.save(CompensationTxLogJpaEntity.fromDomain(compensationTxLog))
                .toDomain();
    }

    @Override
    public List<CompensationTxLog> saveAll(List<CompensationTxLog> compensationTxLogs) {
        return compensationTxLogJpaRepository.saveAll(
                compensationTxLogs.stream()
                        .map(CompensationTxLogJpaEntity::fromDomain)
                        .toList())
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<CompensationTxLog> findById(Long txId) {
        return compensationTxLogJpaRepository.findById(txId)
                .map(CompensationTxLogJpaEntity::toDomain);
    }

    @Override
    public List<CompensationTxLog> findAllByStatus(CompensationTxStatus compensationTxStatus) {
        return compensationTxLogJpaRepository.findAllByStatus(compensationTxStatus.toString())
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<CompensationTxLog> findAllByRetryCountGreaterThanEqual(int targetRetryCount) {
        return compensationTxLogJpaRepository.findAllByRetryCountGreaterThanEqual(targetRetryCount)
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<CompensationTxLog> findAllByRetryCountLessThan(int targetRetryCount) {
        return compensationTxLogJpaRepository.findAllByRetryCountLessThan(targetRetryCount)
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<CompensationTxLog> findAllByUserId(Long userId) {
        return compensationTxLogJpaRepository.findAllByUserId(userId)
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<CompensationTxLog> findAllByReservationId(Long reservationId) {
        return compensationTxLogJpaRepository.findAllByReservationId(reservationId)
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<CompensationTxLog> findAllByUserIdAndReservationId(Long userId, Long reservationId) {
        return compensationTxLogJpaRepository.findAllByUserIdAndReservationId(userId, reservationId)
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<CompensationTxLog> findAllCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return compensationTxLogJpaRepository.findAllByCreatedAtBetween(startDateTime, endDateTime)
                .stream()
                .map(CompensationTxLogJpaEntity::toDomain)
                .toList();
    }
}
