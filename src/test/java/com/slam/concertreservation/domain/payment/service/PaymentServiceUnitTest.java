package com.slam.concertreservation.domain.payment.service;

import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import com.slam.concertreservation.domain.payment.model.Payment;
import com.slam.concertreservation.domain.payment.model.PaymentStatus;
import com.slam.concertreservation.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("initiate 메서드 테스트")
    class InitiatePaymentTest {

        @Test
        @DisplayName("성공 : 결제를 생성하면 PENDING 상태의 Payment가 저장되고 반환된다.")
        void shouldReturnPendingPayment_WhenInitiated() {
            // given
            Long userId = 1L;
            int price = 1000;
            Long reservationId = 1L;
            Payment expected = Payment.create(userId, price, reservationId);

            when(paymentRepository.save(any(Payment.class))).thenReturn(expected);

            // when
            Payment actual = paymentService.initiate(userId, price, reservationId);

            // then
            verify(paymentRepository, times(1)).save(any(Payment.class));
            assertEquals(PaymentStatus.PENDING, actual.getStatus());
            assertEquals(userId, actual.getUserId());
            assertEquals(price, actual.getPrice());
            assertEquals(reservationId, actual.getReservationId());
        }
    }

    @Nested
    @DisplayName("complete 메서드 테스트")
    class CompletePaymentTest {

        @Test
        @DisplayName("성공 : 결제를 완료하면 COMPLETED 상태의 Payment가 저장되고 반환된다.")
        void shouldReturnCompletedPayment_WhenCompleted() {
            // given
            Payment initiatedPayment = Payment.create(1L, 1000, 1L);
            Payment completedPayment = initiatedPayment.withStatus(PaymentStatus.COMPLETED);

            when(paymentRepository.save(any(Payment.class))).thenReturn(completedPayment);

            // when
            Payment actual = paymentService.complete(initiatedPayment);

            // then
            verify(paymentRepository, times(1)).save(any(Payment.class));
            assertEquals(PaymentStatus.COMPLETED, actual.getStatus());
        }
    }

    @Nested
    @DisplayName("fail 메서드 테스트")
    class FailPaymentTest {

        @Test
        @DisplayName("성공 : 결제를 실패 처리하면 FAILED 상태의 Payment가 저장되고 반환된다.")
        void shouldReturnFailedPayment_WhenFailed() {
            // given
            Payment initiatedPayment = Payment.create(1L, 1000, 1L);
            Payment failedPayment = initiatedPayment.withStatus(PaymentStatus.FAILED);

            when(paymentRepository.save(any(Payment.class))).thenReturn(failedPayment);

            // when
            Payment actual = paymentService.fail(initiatedPayment);

            // then
            verify(paymentRepository, times(1)).save(any(Payment.class));
            assertEquals(PaymentStatus.FAILED, actual.getStatus());
        }
    }

    @Nested
    @DisplayName("refund 메서드 테스트")
    class RefundPaymentTest {

        @Test
        @DisplayName("성공 : 결제를 환불 처리하면 REFUNDED 상태의 Payment가 저장되고 반환된다.")
        void shouldReturnRefundedPayment_WhenRefunded() {
            // given
            Payment initiatedPayment = Payment.create(1L, 1000, 1L);
            Payment refundedPayment = initiatedPayment.withStatus(PaymentStatus.REFUNDED);

            when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);

            // when
            Payment actual = paymentService.refund(initiatedPayment);

            // then
            verify(paymentRepository, times(1)).save(any(Payment.class));
            assertEquals(PaymentStatus.REFUNDED, actual.getStatus());
        }
    }

    @Nested
    @DisplayName("getPaymentById 메서드 테스트")
    class GetPaymentByIdTest {

        @Test
        @DisplayName("성공 : 존재하는 Payment ID로 조회하면 해당 Payment를 반환한다.")
        void shouldReturnPayment_WhenPaymentExists() {
            // given
            Long paymentId = 1L;
            Payment expected = Payment.restore(paymentId, 1L, 1L, 1000, PaymentStatus.COMPLETED, null);

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(expected));

            // when
            Payment actual = paymentService.getPaymentById(paymentId);

            // then
            verify(paymentRepository, times(1)).findById(paymentId);
            assertEquals(paymentId, actual.getPaymentId());
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 Payment ID로 조회하면 UnavailableRequestException이 발생하며 실패한다.")
        void shouldThrowUnavailableRequestException_WhenPaymentNotFound() {
            // given
            Long paymentId = 999L;

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                    .isInstanceOf(UnavailableRequestException.class);
        }
    }

    @Nested
    @DisplayName("getPaymentHistoryOfUser 메서드 테스트")
    class GetPaymentHistoryOfUserTest {

        @Test
        @DisplayName("성공 : 사용자의 결제 이력을 페이지 형태로 반환한다.")
        void shouldReturnPageOfPayments_WhenUserHasPayments() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Payment payment1 = Payment.restore(1L, userId, 1L, 1000, PaymentStatus.COMPLETED, null);
            Payment payment2 = Payment.restore(2L, userId, 2L, 2000, PaymentStatus.REFUNDED, null);
            Page<Payment> expectedPage = new PageImpl<>(List.of(payment1, payment2), pageable, 2);

            when(paymentRepository.findAllByUserId(userId, pageable)).thenReturn(expectedPage);

            // when
            Page<Payment> actual = paymentService.getPaymentHistoryOfUser(userId, pageable);

            // then
            verify(paymentRepository, times(1)).findAllByUserId(userId, pageable);
            assertEquals(2, actual.getContent().size());
        }
    }
}
