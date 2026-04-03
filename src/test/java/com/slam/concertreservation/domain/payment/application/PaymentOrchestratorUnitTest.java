package com.slam.concertreservation.domain.payment.application;

import com.slam.concertreservation.domain.payment.model.Payment;
import com.slam.concertreservation.domain.payment.model.PaymentStatus;
import com.slam.concertreservation.domain.payment.service.CompensationTxLogService;
import com.slam.concertreservation.domain.payment.service.PaymentService;
import com.slam.concertreservation.domain.point.api.PointModuleApi;
import com.slam.concertreservation.domain.point.api.PointOperationResult;
import com.slam.concertreservation.domain.reservation.api.ReservationModuleApi;
import com.slam.concertreservation.domain.reservation.api.ReservationOperationResult;
import com.slam.concertreservation.interfaces.dto.PaymentProcessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

class PaymentOrchestratorUnitTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private ReservationModuleApi reservationModuleApi;

    @Mock
    private PointModuleApi pointModuleApi;

    @Mock
    private CompensationTxLogService compensationTxLogService;

    @InjectMocks
    private PaymentOrchestrator paymentOrchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("processPayment 메서드 테스트")
    class ProcessPaymentTest {

        @Test
        @DisplayName("성공 : 포인트 차감과 예약 확정이 모두 성공하면 결제가 완료된다.")
        void shouldCompletePayment_WhenAllOperationsSucceed() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedPayment = Payment.create(userId, price, reservationId);
            Payment completedPayment = initiatedPayment.withStatus(PaymentStatus.COMPLETED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedPayment);
            given(pointModuleApi.decreaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(reservationModuleApi.confirmReservation(reservationId))
                    .willReturn(ReservationOperationResult.success(reservationId));
            given(paymentService.complete(initiatedPayment)).willReturn(completedPayment);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processPayment(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.COMPLETED, response.getPaymentStatus());
            verify(paymentService).complete(initiatedPayment);
            verify(compensationTxLogService, never()).log(anyLong(), anyLong(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("실패 : 포인트 차감에 실패하면 결제가 실패하고 예약 확정을 시도하지 않는다.")
        void shouldFailPayment_WhenPointDeductionFails() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedPayment = Payment.create(userId, price, reservationId);
            Payment failedPayment = initiatedPayment.withStatus(PaymentStatus.FAILED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedPayment);
            given(pointModuleApi.decreaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.fail(userId, price, "POINT_NOT_ENOUGH"));
            given(paymentService.fail(initiatedPayment)).willReturn(failedPayment);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processPayment(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
            verify(reservationModuleApi, never()).confirmReservation(anyLong());
            verify(paymentService).fail(initiatedPayment);
        }

        @Test
        @DisplayName("실패 : 예약 확정에 실패하면 포인트 차감을 보상하고 결제가 실패한다.")
        void shouldCompensatePointAndFailPayment_WhenReservationConfirmationFails() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedPayment = Payment.create(userId, price, reservationId);
            Payment failedPayment = initiatedPayment.withStatus(PaymentStatus.FAILED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedPayment);
            given(pointModuleApi.decreaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(reservationModuleApi.confirmReservation(reservationId))
                    .willReturn(ReservationOperationResult.fail(reservationId, "RESERVATION_EXPIRED"));
            // 보상 트랜잭션 : 포인트 복구
            given(pointModuleApi.increaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(paymentService.fail(initiatedPayment)).willReturn(failedPayment);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processPayment(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
            verify(pointModuleApi).increaseUserPointBalance(userId, price); // 보상 로직 호출 검증
            verify(paymentService).fail(initiatedPayment);
        }

        @Test
        @DisplayName("실패 : 예약 확정 실패 후 포인트 보상까지 실패하면 보상 트랜잭션 로그를 남긴다.")
        void shouldLogCompensationTx_WhenCompensationFails() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedPayment = Payment.create(userId, price, reservationId);
            Payment failedPayment = initiatedPayment.withStatus(PaymentStatus.FAILED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedPayment);
            given(pointModuleApi.decreaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(reservationModuleApi.confirmReservation(reservationId))
                    .willReturn(ReservationOperationResult.fail(reservationId, "RESERVATION_EXPIRED"));
            // 보상 트랜잭션 실패 시뮬레이션
            given(pointModuleApi.increaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.fail(userId, price, "SYSTEM_ERROR"));
            given(paymentService.fail(initiatedPayment)).willReturn(failedPayment);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processPayment(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
            verify(compensationTxLogService).log(userId, reservationId, initiatedPayment.getPaymentId(), price);
        }
    }

    @Nested
    @DisplayName("processRefund 메서드 테스트")
    class ProcessRefundTest {

        @Test
        @DisplayName("성공 : 포인트 환불과 예약 취소가 모두 성공하면 환불이 완료된다.")
        void shouldCompleteRefund_WhenAllOperationsSucceed() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedRefund = Payment.create(userId, price, reservationId);
            Payment refundedPayment = initiatedRefund.withStatus(PaymentStatus.REFUNDED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedRefund);
            given(pointModuleApi.increaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(reservationModuleApi.cancelReservation(reservationId))
                    .willReturn(ReservationOperationResult.success(reservationId));
            given(paymentService.refund(initiatedRefund)).willReturn(refundedPayment);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processRefund(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.REFUNDED, response.getPaymentStatus());
            verify(paymentService).refund(initiatedRefund);
            verify(compensationTxLogService, never()).log(anyLong(), anyLong(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("실패 : 포인트 환불(증가)에 실패하면 환불 처리가 실패하고 예약 취소를 시도하지 않는다.")
        void shouldFailRefund_WhenPointRestorationFails() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedRefund = Payment.create(userId, price, reservationId);
            Payment failedRefund = initiatedRefund.withStatus(PaymentStatus.FAILED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedRefund);
            given(pointModuleApi.increaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.fail(userId, price, "SYSTEM_ERROR"));
            given(paymentService.fail(initiatedRefund)).willReturn(failedRefund);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processRefund(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
            verify(reservationModuleApi, never()).cancelReservation(anyLong());
            verify(paymentService).fail(initiatedRefund);
        }

        @Test
        @DisplayName("실패 : 예약 취소에 실패하면 포인트 증가를 보상(차감)하고 환불 처리가 실패한다.")
        void shouldCompensatePointAndFailRefund_WhenReservationCancellationFails() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedRefund = Payment.create(userId, price, reservationId);
            Payment failedRefund = initiatedRefund.withStatus(PaymentStatus.FAILED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedRefund);
            given(pointModuleApi.increaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(reservationModuleApi.cancelReservation(reservationId))
                    .willReturn(ReservationOperationResult.fail(reservationId, "SYSTEM_ERROR"));
            // 보상 트랜잭션 : 포인트 차감
            given(pointModuleApi.decreaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(paymentService.fail(initiatedRefund)).willReturn(failedRefund);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processRefund(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
            verify(pointModuleApi).decreaseUserPointBalance(userId, price); // 보상 로직 호출 검증
            verify(paymentService).fail(initiatedRefund);
        }

        @Test
        @DisplayName("실패 : 예약 취소 실패 후 포인트 보상(차감)까지 실패하면 보상 트랜잭션 로그를 남긴다.")
        void shouldLogCompensationTx_WhenRefundCompensationFails() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment initiatedRefund = Payment.create(userId, price, reservationId);
            Payment failedRefund = initiatedRefund.withStatus(PaymentStatus.FAILED);

            given(paymentService.initiate(userId, price, reservationId)).willReturn(initiatedRefund);
            given(pointModuleApi.increaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.success(userId, price));
            given(reservationModuleApi.cancelReservation(reservationId))
                    .willReturn(ReservationOperationResult.fail(reservationId, "SYSTEM_ERROR"));
            // 보상 트랜잭션 실패 시뮬레이션
            given(pointModuleApi.decreaseUserPointBalance(userId, price))
                    .willReturn(PointOperationResult.fail(userId, price, "POINT_NOT_ENOUGH"));
            given(paymentService.fail(initiatedRefund)).willReturn(failedRefund);

            // when
            PaymentProcessResponse response = paymentOrchestrator.processRefund(userId, price, reservationId);

            // then
            assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
            // compensatePointIncrease 에서는 -price 로 로그를 남김
            verify(compensationTxLogService).log(userId, reservationId, initiatedRefund.getPaymentId(), -price);
        }
    }
}
