package com.slam.concertreservation.domain.queue.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenUnitTest {

    @Test
    @DisplayName("성공 : 토큰 최초 생성 시의 상태는 WAIT이다.")
    void shouldBeWaiting_WhenTokenIsCreated() {
        // given
        Token token = Token.create("userId", "concertScheduleId", 1);

        // when & then
        Assertions.assertEquals(TokenStatus.WAIT, token.getStatus());
    }


    @Test
    @DisplayName("성공 : 토큰 만료 시간 설정이 정상적으로 이루어진다.")
    void shouldBeSetExpiredAt_WhenTokenIsInitiated() {
        // given
        Token token = Token.create("userId", "concertScheduleId", 1);
        token.activate(1);
        LocalDateTime expiration = LocalDateTime.now();

        // when
        token.initiateExpiredAt(expiration);

        // then
        Assertions.assertEquals(expiration, token.getExpiredAt());
    }
}
