package com.hhp7.concertreservation.domain.queue.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenUnitTest {

    @Test
    @DisplayName("성공 : 토큰 최초 생성 시의 상태는 WAIT이다.")
    void shouldBeWaiting_WhenTokenIsCreated() {
        // given
        Token token = Token.create("userId", "concertScheduleId");

        // when & then
        Assertions.assertEquals(TokenStatus.WAIT, token.getStatus());
    }

    @Test
    @DisplayName("성공 : 토큰 상태가 WAIT 일때만 활성화 가능하다.")
    void shouldBeActivated_WhenTokenIsWaiting() {
        // given
        Token token = Token.create("userId", "concertScheduleId");

        // when
        token.activate();

        // then
        Assertions.assertEquals(TokenStatus.ACTIVE, token.getStatus());
    }

    @Test
    @DisplayName("실패 : 토큰 상태가 ACTIVE 일때 활성화 시도 시 예외가 발생한다.")
    void shouldThrowException_WhenTokenIsAlreadyActivated() {
        // given
        Token token = Token.create("userId", "concertScheduleId");
        token.activate();

        // when & then
        assertThatThrownBy(() -> token.activate())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("이미 활성화된 토큰입니다.");
    }

    @Test
    @DisplayName("성공 : 토큰 상태가 ACTIVE 일때만 만료 가능하다.")
    void shouldBeExpired_WhenTokenIsActive() {
        // given
        Token token = Token.create("userId", "concertScheduleId");
        token.activate();

        // when
        token.expire();

        // then
        Assertions.assertEquals(TokenStatus.EXPIRED, token.getStatus());
    }

    @Test
    @DisplayName("실패 : 토큰 상태가 EXPIRED 일때 만료 시도 시 예외가 발생한다.")
    void shouldThrowException_WhenTokenIsAlreadyExpired() {
        // given
        Token token = Token.create("userId", "concertScheduleId");
        token.activate();
        token.expire();

        // when & then
        assertThatThrownBy(() -> token.expire())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("이미 만료된 토큰입니다.");
    }

    @Test
    @DisplayName("성공 : 토큰 만료 시간 설정이 정상적으로 이루어진다.")
    void shouldBeSetExpiredAt_WhenTokenIsInitiated() {
        // given
        Token token = Token.create("userId", "concertScheduleId");
        token.activate();
        LocalDateTime expiration = LocalDateTime.now();

        // when
        token.initiateExpiredAt(expiration);

        // then
        Assertions.assertEquals(expiration, token.getExpiredAt());
    }
}
