package com.hhp7.concertreservation.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class QueueServiceIntegrationTest {

    @Nested
    class NonConcurrentScenario {
        @Test
        @DisplayName("성공 : 어떤 사용자가 대기열에 진입하면 대기열 토큰이 발급된다.")
        void shouldSuccessIssueToken_WhenEnterQueue() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("성공 : 한 번에 K개의 대기열 토큰 상태를 '활성화'로 변경할 수 있다.")
        void shouldSuccessActivateKTokens_WhenActivateKTokensAtOnce() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("성공 : 한 번에 K 개의 대기열 토큰을 만료 시킬 수 있다.")
        void shouldSuccessExpireKTokens_WhenExpireKTokensAtOnce() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("성공 : 어떤 사용자의 대기열 토큰 상태가 '활성화'로 변경되면 서비스 진입에 성공한다.")
        void shouldSuccessEnterService_WhenTokenStatusIsChangedToActive() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("실패 : 어떤 사용자의 대기열 토큰 상태가 '비활성화'인 경우 서비스 진입에 실패한다.")
        void shouldFailEnterService_WhenTokenStatusIsInactive() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("성공 : 어떤 사용자의 대기열 순번을 조회할 수 있다.")
        void shouldSuccessGetQueueNumber_WhenGetQueueNumber() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("성공 : 현재 활성화된 대기열 토큰의 수를 조회할 수 있다.")
        void shouldSuccessGetNumberOfActiveTokens_WhenGetNumberOfActiveTokens() {
            // given
            // when
            // then
        }
    }

    @Nested
    class ConcurrentScenario {
        @Test
        @DisplayName("성공 : 동시에 100명 대기열 진입 시 100개의 대기열 토큰 발급에 성공한다.")
        void shouldSuccessIssuing100Tokens_When100PeopleEnterQueue() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("성공 : 9명이 순서대로 대기열에 진입한다. 이때 매번 마지막 사용자의 순번을 조회하는데, 이 순번은 1씩 감소한다.")
        void shouldSuccessGetQueueNumber_When9PeopleEnterQueueConcurrently() {
            // given
            // when
            // then
        }
    }
}
