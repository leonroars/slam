package com.hhp7.concertreservation.domain.concert.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import net.bytebuddy.asm.Advice.Local;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConcertScheduleUnitTest {

    // 시점이 빠른 순으로 세 가지 일정을 준비한다.
    private static final LocalDateTime FIRST = LocalDateTime.of(2024, 1, 1, 10, 0);
    private static final LocalDateTime SECOND = LocalDateTime.of(2024, 1, 1, 11, 0);
    private static final LocalDateTime THIRD = LocalDateTime.of(2024, 1, 1, 12, 0);

    @Test
    @DisplayName("실패 : 예약 시작 시점 이전에 공연이 시작하는 공연일정 생성 시도 시 BusinessRuleViolationException 발생하며 실패한다.")
    void shouldThrowBusinessRuleViolationException_WhenCocnertStartsBeforeItsReservationStarts(){
        // given
        LocalDateTime dateTime = FIRST; // 공연 시작 일자
        LocalDateTime reservationStartAt = SECOND;
        LocalDateTime reservationEndAt = THIRD;

        // when & then
        Assertions.assertThatThrownBy(
                () -> ConcertSchedule.create("1", dateTime, reservationStartAt, reservationEndAt)
        ).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("실패 : 예약 종료 시점 이전에 공연이 시작하는 공연 일정 생성 시도 시 BusinessRuleViolationException 발생하며 실패한다.")
    void shouldThrowBusinessRuleViolationException_WhenCocnertStartsBeforeItsReservationEnds(){
        // given
        LocalDateTime dateTime = FIRST; // 공연 시작 일자
        LocalDateTime reservationStartAt = SECOND;
        LocalDateTime reservationEndAt = THIRD;

        // when & then
        Assertions.assertThatThrownBy(
                () -> ConcertSchedule.create("1", dateTime, reservationStartAt, reservationEndAt)
        ).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 적법한 공연 일정 생성은 성공한다.")
    void shouldSuccessfullyCreateConcertSchedule_WhenItIsLegal(){
        // given
        LocalDateTime dateTime = THIRD; // 공연 시작 일자
        LocalDateTime reservationStartAt = FIRST;
        LocalDateTime reservationEndAt = SECOND;

        // when & then
        Assertions.assertThatCode(
                () -> ConcertSchedule.create("1", dateTime, reservationStartAt, reservationEndAt)
        ).doesNotThrowAnyException();

    }

    @Test
    @DisplayName("실패 : 특정 공연 일정의 예약 가능 인원 수가 0일 때 인원 수 감소 시도할 경우 BusinessRuleViolationException 발생하며 실패한다.")
    void shouldThrowBusinessRuleViolationException_WhenDecrementOccursTowardZero(){
        // given : 예약 가능 인원 수가 0인 공연 일정이 존재한다.
        ConcertSchedule concertSchedule = ConcertSchedule.create("1", THIRD, FIRST, SECOND);

        for(int i = ConcertSchedule.MAX_AVAILABLE_SEATS; i > ConcertSchedule.MIN_AVAILABLE_SEATS; i--){
            concertSchedule.decrementAvailableSeatCount();
        }

        // when & then : 해당 공연 일정의 예약 가능 인원 수 차감을 시도한다.
        Assertions.assertThatThrownBy(
                concertSchedule::decrementAvailableSeatCount
        ).isInstanceOf(BusinessRuleViolationException.class);

    }

    @Test
    @DisplayName("실패 : 특정 공연 일정의 예약 가능 인원 수가 최대 한도(현재 50석)일때, 추가 시도가 있을 경우 BusinessRuleException 발생하며 실패한다.")
    void shouldThrowBusinessRuleViolationException_WhenIncrementOccursTowardMax(){
        // given : 예약 가능 인원 수가 최대(50 석)인 공연 일정 존재.
        ConcertSchedule concertSchedule = ConcertSchedule.create("1", THIRD, FIRST, SECOND);

        // when & then
        Assertions.assertThatThrownBy(
                concertSchedule::incrementAvailableSeatCount
        ).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 특정 공연 일정에 대해 적법한 '예약 가능 인원 수' 증가 시도는 성공한다.")
    void shouldSuccessfullyIncreaseAvailableSeatsCount_WhenItIsLegal(){
        // given : 예약 가능 인원 수가 1인 공연 일정이 존재한다.
        ConcertSchedule concertSchedule = ConcertSchedule.create("1", THIRD, FIRST, SECOND);
        concertSchedule.decrementAvailableSeatCount();

        // when & then
        Assertions.assertThatCode(
                concertSchedule::incrementAvailableSeatCount
        ).doesNotThrowAnyException();

    }

    @Test
    @DisplayName("성공 : 특정 공연 일정에 대해 적법한 '예약 가능 인원 수' 감소 시도는 성공한다.")
    void shouldSuccessfullyDecreaseAvailableSeatsCount_WhenItIsLegal(){
        // given : 예약 가능 인원 수가 0 초과인 공연 일정이 존재한다.
        ConcertSchedule concertSchedule = ConcertSchedule.create("1", THIRD, FIRST, SECOND);

        // when & then
        Assertions.assertThatCode(
                concertSchedule::decrementAvailableSeatCount
        ).doesNotThrowAnyException();

    }
}
