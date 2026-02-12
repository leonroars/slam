package com.slam.concertreservation.domain.payment.service;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import com.slam.concertreservation.domain.payment.model.CompensationTxLog;
import com.slam.concertreservation.domain.payment.model.CompensationTxStatus;
import com.slam.concertreservation.domain.payment.repository.CompensationTxLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompensationTxLogService {

    private final CompensationTxLogRepository compensationTxLogRepository;

    public CompensationTxLog log(Long userId, Long reservationId, Long paymentId, int price) {
        CompensationTxLog txLog = CompensationTxLog.create(userId, reservationId, paymentId, price);
        return compensationTxLogRepository.save(txLog);
    }

    public CompensationTxLog markAsFailed(CompensationTxLog compensationTxLog) {
        compensationTxLog.markAsFailed();
        return compensationTxLogRepository.save(compensationTxLog);
    }

    public CompensationTxLog markAsCompleted(CompensationTxLog compensationTxLog) {
        compensationTxLog.markAsCompleted();
        return compensationTxLogRepository.save(compensationTxLog);
    }

    public List<CompensationTxLog> getAllRetriables() {
        return compensationTxLogRepository.findAllByStatus(CompensationTxStatus.PENDING);
    }

    public CompensationTxLog getById(Long txLogId) {
        return compensationTxLogRepository.findById(txLogId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.RESOURCE_NOT_FOUND, "CompensationTxLog not found for id: " + txLogId));
    }

    public List<CompensationTxLog> getByStatus(CompensationTxStatus status) {
        return compensationTxLogRepository.findAllByStatus(status);
    }

    public List<CompensationTxLog> getByRetryCountGreaterThanEqualToMax() {
        return compensationTxLogRepository.findAllByRetryCountGreaterThanEqual(CompensationTxLog.MAX_RETRY_COUNT);
    }

    public List<CompensationTxLog> getByUserId(Long userId) {
        return compensationTxLogRepository.findAllByUserId(userId);
    }

    public List<CompensationTxLog> getByReservationId(Long reservationId) {
        return compensationTxLogRepository.findAllByReservationId(reservationId);
    }

    public List<CompensationTxLog> getByUserIdAndReservationId(Long userId, Long reservationId) {
        return compensationTxLogRepository.findAllByUserIdAndReservationId(userId, reservationId);
    }

    public List<CompensationTxLog> getCompensationTxLogsCreatedAtBetween(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {
        return compensationTxLogRepository.findAllCreatedAtBetween(startDateTime, endDateTime);
    }

    public List<CompensationTxLog> getByRetryCountLessThan(int targetRetryCount) {
        return compensationTxLogRepository.findAllByRetryCountLessThan(targetRetryCount);
    }

    public List<CompensationTxLog> getByRetryCountGreaterThanEqual(int targetRetryCount) {
        return compensationTxLogRepository.findAllByRetryCountGreaterThanEqual(targetRetryCount);
    }

}
