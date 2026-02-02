package com.slam.concertreservation.domain.reservation.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReservationUnitTest {

    @Test
    @DisplayName("성공 : 예약 생성 및 초기화 시 해당 예약의 상태는 PREEMTED로 초기화된다.")
    void shouldInitializeReservationStatusAsPreempted_WhenReservationCreated() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);

        // when & then
        Assertions.assertEquals(ReservationStatus.PREEMPTED, reservation.getStatus());
    }

    @Test
    @DisplayName("성공 : 예약 상태가 PREEMTED일 때만 CONFIRMED로 변경 가능하다.")
    void shouldChangeStatusToPaid_WhenStatusIsPreempted() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);

        // when
        reservation.confirm();

        // when & then
        Assertions.assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    @DisplayName("실패 : PREEMPTED 가 아닌 다른 상태의 예약을 CONFIRMED로 변경하려고 시도할 경우 BusinessRuleViolationException 발생")
    void shouldThrowBusinessRuleViolationException_WhenTryToChangeStatusToConfirmed_WhenStatusIsNotPreempted() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);
        reservation.confirm();

        // when & then
        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 예약 상태가 PREEMPTED일 때만 EXPIRED로 변경 가능하다.")
    void shouldChangeStatusToExpired_WhenStatusIsPreempted() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);

        // when
        reservation.expire();

        // when & then
        Assertions.assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
    }

    @Test
    @DisplayName("실패 : PREEMPTED 가 아닌 다른 상태의 예약을 EXPIRED로 변경하려고 시도할 경우 BusinessRuleViolationException 발생")
    void shouldThrowBusinessRuleViolationException_WhenTryToChangeStatusToExpired_WhenStatusIsNotPREEMPTED() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);
        reservation.confirm();

        // when & then
        assertThatThrownBy(reservation::expire)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 예약 상태가 CONFIRMED일 때만 CANCELLED로 변경 가능하다.")
    void shouldChangeStatusToCancelled_WhenStatusIsConfirmed() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);
        reservation.confirm();

        // when
        reservation.cancel();

        // when & then
        Assertions.assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    @DisplayName("실패 : CONFIRMED 가 아닌 다른 상태의 예약을 CANCELLED로 변경하려고 시도할 경우 BusinessRuleViolationException 발생")
    void shouldThrowBusinessRuleViolationException_WhenTryToChangeStatusToCancelled_WhenStatusIsNotConfirmed() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);

        // when & then
        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : PREEMPTED 상태의 예약에 대한 결제 요청 진입 시, 예약을 PAYMENT_PENDING 으로 변경 가능하다.")
    void shouldChangeStatusToPaymentPending_WhenStatusIsPreempted() {
        // given
        Reservation reservation = Reservation.create(1L, 1L, 1L, 1L, 1);

        // when
        reservation.beginPayment();

        // then
        Assertions.assertEquals(ReservationStatus.PAYMENT_PENDING, reservation.getStatus());
    }
}
