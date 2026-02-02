package com.slam.concertreservation.domain.concert.model;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
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
    void shouldThrowBusinessRuleViolationException_WhenCocnertStartsBeforeItsReservationStarts() {
        // given
        LocalDateTime dateTime = FIRST; // 공연 시작 일자
        LocalDateTime reservationStartAt = SECOND;
        LocalDateTime reservationEndAt = THIRD;

        // when & then
        Assertions.assertThatThrownBy(
                () -> ConcertSchedule.create(1L, dateTime, reservationStartAt, reservationEndAt))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("실패 : 예약 종료 시점 이전에 공연이 시작하는 공연 일정 생성 시도 시 BusinessRuleViolationException 발생하며 실패한다.")
    void shouldThrowBusinessRuleViolationException_WhenCocnertStartsBeforeItsReservationEnds() {
        // given
        LocalDateTime dateTime = FIRST; // 공연 시작 일자
        LocalDateTime reservationStartAt = SECOND;
        LocalDateTime reservationEndAt = THIRD;

        // when & then
        Assertions.assertThatThrownBy(
                () -> ConcertSchedule.create(1L, dateTime, reservationStartAt, reservationEndAt))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 적법한 공연 일정 생성은 성공한다.")
    void shouldSuccessfullyCreateConcertSchedule_WhenItIsLegal() {
        // given
        LocalDateTime dateTime = THIRD; // 공연 시작 일자
        LocalDateTime reservationStartAt = FIRST;
        LocalDateTime reservationEndAt = SECOND;

        // when & then
        Assertions.assertThatCode(
                () -> ConcertSchedule.create(1L, dateTime, reservationStartAt, reservationEndAt))
                .doesNotThrowAnyException();

    }
}
