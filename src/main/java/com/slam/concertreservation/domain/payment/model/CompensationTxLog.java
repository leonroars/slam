package com.slam.concertreservation.domain.payment.model;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import io.hypersistence.tsid.TSID;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 보상 트랜잭션 로그 모델
 */
@Getter
public class CompensationTxLog {

    public static final int MAX_RETRY_COUNT = 3;

    private Long txLogId;
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private int price;
    private CompensationTxStatus status;
    private int retryCount;
    private LocalDateTime createdAt;

    private CompensationTxLog(){}
    private CompensationTxLog(
            Long txLogId,
            Long paymentId,
            Long userId,
            Long reservationId,
            int price,
            CompensationTxStatus status,
            int retryCount,
            LocalDateTime createdAt) {
        this.txLogId = txLogId;
        this.paymentId = paymentId;
        this.userId = userId;
        this.reservationId = reservationId;
        this.price = price;
        this.status = status;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
    }


    public static CompensationTxLog create(Long userId, Long reservationId, Long paymentId, int price){
        CompensationTxLog txLog = new CompensationTxLog();
        txLog.txLogId = TSID.fast().toLong();
        txLog.userId = userId;
        txLog.reservationId = reservationId;
        txLog.paymentId = paymentId;
        txLog.price = price;
        txLog.status = CompensationTxStatus.PENDING;
        txLog.retryCount = 0;
        txLog.createdAt = LocalDateTime.now();
        return txLog;
    }

    public static CompensationTxLog restore(
            Long txLogId,
            Long paymentId,
            Long userId,
            Long reservationId,
            int price,
            CompensationTxStatus status,
            int retryCount,
            LocalDateTime createdAt) {
        return new CompensationTxLog(
                txLogId,
                paymentId,
                userId,
                reservationId,
                price,
                status,
                retryCount,
                createdAt
        );
    }

    public CompensationTxLog markAsCompleted() {
        this.status = CompensationTxStatus.COMPLETED;
        return this;
    }

    public CompensationTxLog markAsFailed() {
        if(this.status == CompensationTxStatus.COMPLETED) {
            throw new BusinessRuleViolationException(ErrorCode.INVALID_REQUEST, "이미 완료 처리된 보상 트랜잭션입니다.");
        }
        this.status = CompensationTxStatus.FAILED;
        this.incrementRetryCount();
        return this;
    }

    private void incrementRetryCount() {++this.retryCount;}
}
