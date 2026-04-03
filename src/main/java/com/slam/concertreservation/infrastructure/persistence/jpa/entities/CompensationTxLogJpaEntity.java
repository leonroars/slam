package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.payment.model.CompensationTxLog;
import com.slam.concertreservation.domain.payment.model.CompensationTxStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class CompensationTxLogJpaEntity {

    @Id
    private Long txLogId;
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private int price;
    private String status;
    private int retryCount;
    private LocalDateTime createdAt;

    public CompensationTxLog toDomain() {
        return CompensationTxLog.restore(
                this.txLogId,
                this.paymentId,
                this.userId,
                this.reservationId,
                this.price,
                CompensationTxStatus.valueOf(this.status),
                this.retryCount,
                this.createdAt
        );
    }

    public static CompensationTxLogJpaEntity fromDomain(CompensationTxLog compensationTxLog){
        CompensationTxLogJpaEntity entity = new CompensationTxLogJpaEntity();
        entity.txLogId = compensationTxLog.getTxLogId();
        entity.paymentId = compensationTxLog.getPaymentId();
        entity.userId = compensationTxLog.getUserId();
        entity.reservationId = compensationTxLog.getReservationId();
        entity.price = compensationTxLog.getPrice();
        entity.status = compensationTxLog.getStatus().name();
        entity.retryCount = compensationTxLog.getRetryCount();
        entity.createdAt = compensationTxLog.getCreatedAt();
        return entity;
    }

}
