package com.slam.concertreservation.domain.payment.repository;

import com.slam.concertreservation.domain.payment.model.CompensationTxLog;
import com.slam.concertreservation.domain.payment.model.CompensationTxStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompensationTxLogRepository {

    CompensationTxLog save(CompensationTxLog compensationTxLog);

    List<CompensationTxLog> saveAll(List<CompensationTxLog> compensationTxLogs);

    Optional<CompensationTxLog> findById(Long txId);

    List<CompensationTxLog> findAllByStatus(CompensationTxStatus compensationTxStatus);

    List<CompensationTxLog> findAllByRetryCountGreaterThanEqual(int targetRetryCount);

    List<CompensationTxLog> findAllByRetryCountLessThan(int targetRetryCount);

    List<CompensationTxLog> findAllByUserId(Long userId);

    List<CompensationTxLog> findAllByReservationId(Long reservationId);

    List<CompensationTxLog> findAllByUserIdAndReservationId(Long userId, Long reservationId);

    List<CompensationTxLog> findAllCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

}
