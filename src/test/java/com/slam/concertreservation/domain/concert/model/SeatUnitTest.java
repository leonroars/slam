package com.slam.concertreservation.domain.concert.model;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SeatUnitTest {

    @Test
    @DisplayName("실패 : 좌석 최소 번호보다 작은 좌석 번호를 가진 Seat 객체 인스턴스 생성 시도 시 BusinessRuleViolation 발생하며 실패.")
    void shouldThrowBusinessRuleViolationException_WhenSeatNumberBelowZero(){
        // given
        int belowMinSeatNumber = -1;

        // when & then
        assertThatThrownBy(() -> Seat.create("1", belowMinSeatNumber, 1000, SeatStatus.AVAILABLE))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("실패 : 좌석 최대 번호보다 큰 좌석 번호를 가진 Seat 객체 인스턴스 생성 시도 시 BusinessRuleViolation 발생하며 실패.")
    void shouldThrowBusinessRuleViolationException_WhenSeatNumberOverMax(){
        // given
        int overMaxSeatNumber = 51;

        // when & then
        assertThatThrownBy(() -> Seat.create("1", overMaxSeatNumber, 1000, SeatStatus.AVAILABLE))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 적법한 좌석 번호를 가진 Seat 객체 인스턴스 생성 시도는 성공한다.")
    void shouldSucceed_WhenSeatNumberIsLegal(){
        // given
        int legalSeatNumber = 2;

        // when & then
        assertThatNoException().isThrownBy(
                () -> Seat.create("1", legalSeatNumber, 1000, SeatStatus.AVAILABLE)
        );
    }

    @Test
    @DisplayName("실패 : 이미 예약 가능한 좌석에 대해 다시 '예약 가능 상태로' 변경 시도 시 BusinessRuleViolationException 발생하며 실패.")
    void shouldThrowBusinessRuleViolationException_WhenThereIsAnAttemptToMakeItAvailable_EvenIfItIsAlreadyAvailable(){
        // given
        Seat seat = Seat.create("1", 1, 1000, SeatStatus.AVAILABLE);

        // when & then
        assertThatThrownBy(seat::makeAvailable)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("실패 : 이미 선점된 좌석에 대해 다시 '선점 상태' 변경 시도 시 BusinessRuleViolationException 발생하며 실패.")
    void shouldThrowBusinessRuleViolationException_WhenThereIsAnAttemptToMakeItOccupied_EvenIfItIsAlreadyOccupied(){
        // given
        Seat seat = Seat.create("1", 1, 1000, SeatStatus.UNAVAILABLE);

        // when & then
        assertThatThrownBy(seat::makeUnavailable)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 예약 가능한 좌석을 선점 상태로 변경하는 것은 성공한다.")
    void shouldSucceed_WhenMakeItOccupied(){
        // given
        Seat seat = Seat.create("1", 1, 1000, SeatStatus.AVAILABLE);

        // when & then
        assertThatNoException().isThrownBy(seat::makeUnavailable);
    }

    @Test
    @DisplayName("성공 : 선점된 좌석을 예약 가능 상태로 변경하는 것은 성공한다.")
    void shouldSucceed_WhenMakeItAvailable(){
        // given
        Seat seat = Seat.create("1", 1, 1000, SeatStatus.UNAVAILABLE);

        // when & then
        assertThatNoException().isThrownBy(seat::makeAvailable);
    }
}
