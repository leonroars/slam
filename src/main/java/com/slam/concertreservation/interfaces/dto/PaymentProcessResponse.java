package com.slam.concertreservation.interfaces.dto;

import com.slam.concertreservation.domain.payment.model.Payment;
import com.slam.concertreservation.domain.payment.model.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentProcessResponse {
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private PaymentStatus paymentStatus;

    public static PaymentProcessResponse from(Payment payment) {
        return PaymentProcessResponse.builder()
                .paymentId(payment.getPaymentId())
                .userId(payment.getUserId())
                .reservationId(payment.getReservationId())
                .paymentStatus(payment.getStatus())
                .build();
    }
}
