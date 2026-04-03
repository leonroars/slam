package com.slam.concertreservation.domain.payment.application;

import com.slam.concertreservation.domain.payment.service.CompensationTxLogService;
import com.slam.concertreservation.domain.point.api.PointOperationResult;
import com.slam.concertreservation.domain.reservation.api.ReservationOperationResult;
import com.slam.concertreservation.interfaces.dto.PaymentProcessResponse;
import com.slam.concertreservation.domain.payment.model.Payment;
import com.slam.concertreservation.domain.payment.service.PaymentService;
import com.slam.concertreservation.domain.point.api.PointModuleApi;
import com.slam.concertreservation.domain.reservation.api.ReservationModuleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final PaymentService paymentService;

    /** 외부 모듈 인터페이스 **/
    private final ReservationModuleApi reservationModuleApi;
    private final PointModuleApi pointModuleApi;

    private final CompensationTxLogService compensationTxLogService;

    public PaymentProcessResponse processPayment(Long userId, int price, Long reservationId) {
        // 1. PENDING 상태의 Payment 생성
        Payment initiatedPayment = paymentService.initiate(userId, price, reservationId);

        // 2. 포인트 차감
        PointOperationResult deduction = pointModuleApi.decreaseUserPointBalance(userId, price);
        if (!deduction.success()) {
            return PaymentProcessResponse.from(paymentService.fail(initiatedPayment));
        }

        // 3. 예약 확정
        ReservationOperationResult confirmation = reservationModuleApi.confirmReservation(reservationId);
        if (!confirmation.success()) {
            compensatePointDeduction(
                    initiatedPayment.getUserId(),
                    initiatedPayment.getReservationId(),
                    initiatedPayment.getPaymentId(),
                    initiatedPayment.getPrice());

            return PaymentProcessResponse.from(paymentService.fail(initiatedPayment));
        }

        // 4. 결제 확정
        return PaymentProcessResponse.from(paymentService.complete(initiatedPayment));
    }

    public PaymentProcessResponse processRefund(Long userId, int price, Long reservationId) {
        // 1. PENDING 상태의 Payment 생성
        Payment initiatedRefund = paymentService.initiate(userId, price, reservationId);

        // 2. 포인트 원상 복구
        PointOperationResult restoration = pointModuleApi.increaseUserPointBalance(userId, price);
        if (!restoration.success()) {
            return PaymentProcessResponse.from(paymentService.fail(initiatedRefund));
        }

        // 3. 예약 취소
        ReservationOperationResult cancellation = reservationModuleApi.cancelReservation(reservationId);
        if (!cancellation.success()) {
            compensatePointIncrease(
                    initiatedRefund.getUserId(),
                    initiatedRefund.getReservationId(),
                    initiatedRefund.getPaymentId(),
                    initiatedRefund.getPrice());

            return PaymentProcessResponse.from(paymentService.fail(initiatedRefund));
        }

        // 4. 환불 확정
        return PaymentProcessResponse.from(paymentService.refund(initiatedRefund));
    }

    private void compensatePointDeduction(Long userId, Long reservationId, Long paymentId, int price) {
        PointOperationResult result = pointModuleApi.increaseUserPointBalance(userId, price);
        if (!result.success()) {
            compensationTxLogService.log(userId, reservationId, paymentId, price);
        }
    }

    private void compensatePointIncrease(Long userId, Long reservationId, Long paymentId, int price) {
        PointOperationResult result = pointModuleApi.decreaseUserPointBalance(userId, price);
        if (!result.success()) {
            compensationTxLogService.log(userId, reservationId, paymentId, -price);
        }
    }
}
