package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.payment.model.Payment;
import com.slam.concertreservation.domain.payment.model.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class PaymentJpaEntity {

    @Id
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private int price;
    private String paymentStatus;
    private LocalDateTime createdAt;

    public Payment toDomain() {
        return Payment.restore(
                this.paymentId,
                this.userId,
                this.reservationId,
                this.price,
                PaymentStatus.valueOf(this.paymentStatus),
                this.createdAt
        );
    }

    public static PaymentJpaEntity fromDomain(Payment payment){
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.paymentId = payment.getPaymentId();
        entity.userId = payment.getUserId();
        entity.reservationId = payment.getReservationId();
        entity.price = payment.getPrice();
        entity.paymentStatus = payment.getStatus().name();
        entity.createdAt = payment.getCreatedAt();
        return entity;
    }
}
