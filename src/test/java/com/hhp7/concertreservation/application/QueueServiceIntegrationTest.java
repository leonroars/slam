package com.hhp7.concertreservation.application;

import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.service.ConcertService;
import com.hhp7.concertreservation.domain.queue.service.QueueService;
import com.hhp7.concertreservation.domain.user.model.User;
import com.hhp7.concertreservation.domain.user.service.UserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class QueueServiceIntegrationTest {

    private final String CONCERT_SCHEDULE_ID = "concertScheduleId";
    private final LocalDateTime CONCERT_SCHEDULE_START_TIME = LocalDateTime.now().plusDays(2);
    private final LocalDateTime CONCERT_SCHEDULE_RESERVATION_START_TIME = LocalDateTime.now().plusDays(1).minusHours(3);
    private final LocalDateTime CONCERT_SCHEDULE_RESERVATION_END_TIME = LocalDateTime.now().plusDays(1).minusHours(1);

    @Autowired
    private QueueService queueService;

    @Autowired
    private ConcertService concertService;

    @Autowired
    private UserService userService;


    @Nested
    class NonConcurrentScenario {

        @AfterEach
        void tearDown() {

        }

        @Test
        @DisplayName("성공 : 어떤 사용자가 대기열에 진입하면 대기열 토큰이 발급된다.")
        void shouldSuccessIssueToken_WhenEnterQueue() {
            // given : 어떤 사용자와 공연 일정 존재.
            User user = User.create("user1");
            ConcertSchedule concertSchedule
                    = ConcertSchedule.create(CONCERT_SCHEDULE_ID, CONCERT_SCHEDULE_START_TIME, CONCERT_SCHEDULE_RESERVATION_START_TIME, CONCERT_SCHEDULE_RESERVATION_END_TIME);

            // when : 해당 사용자가 해당 공연 일정 대기열 진입

            // then : 대기열 토큰이 발급된다.

        }

        @Test
        @DisplayName("성공 : 한 번에 K개의 대기열 토큰 상태를 '활성화'로 변경할 수 있다.")
        void shouldSuccessActivateKTokens_WhenActivateKTokensAtOnce() {
            // given : K개의 대기열 토큰이 존재하고, 모두 '대기' 상태.

            // when : K개의 대기열 토큰 상태를 '활성화'로 변경

            // then : K개의 대기열 토큰 상태가 '활성화'로 변경된다.
        }

        @Test
        @DisplayName("성공 : 한 번에 K 개의 대기열 토큰을 만료 시킬 수 있다.")
        void shouldSuccessExpireKTokens_WhenExpireKTokensAtOnce() {
            // given : K개의 대기열 토큰이 존재하고, 모두 '활성화' 상태.

            // when : K개의 대기열 토큰 상태를 '만료'로 변경

            // then : K개의 대기열 토큰 상태가 '만료'로 변경된다.
        }

        @Test
        @DisplayName("성공 : 어떤 사용자의 대기열 토큰 상태가 '활성화'로 변경되면 서비스 진입에 성공한다.")
        void shouldSuccessEnterService_WhenTokenStatusIsChangedToActive() {
            // given : 어떤 사용자의 대기열 토큰이 '활성화' 상태로 변경된 경우

            // when & then : 토큰 검증을 호출하고, 검증 결과는 성공이다.
        }

        @Test
        @DisplayName("실패 : 어떤 사용자의 대기열 토큰 상태가 '비활성화'인 경우 서비스 진입에 실패한다.")
        void shouldFailEnterService_WhenTokenStatusIsInactive() {
            // given : 어떤 사용자의 대기열 토큰이 '비활성화' 상태

            // when & then : 토큰 검증을 호출 시 실패 결과를 반환한다.
        }

        @Test
        @DisplayName("성공 : 어떤 사용자의 대기열 순번을 조회할 수 있다.")
        void shouldSuccessGetQueueNumber_WhenGetQueueNumber() {
            // given : 어떤 사용자의 대기열 토큰이 존재하고, '대기' 상태. 앞에 10명의 사용자가 대기 중.

            // when : 해당 사용자의 대기열 순번을 조회한다.

            // then : 해당 사용자의 대기열 순번이 11이다.
        }

        @Test
        @DisplayName("성공 : 현재 활성화된 대기열 토큰의 수를 조회할 수 있다.")
        void shouldSuccessGetNumberOfActiveTokens_WhenGetNumberOfActiveTokens() {
            // given : 현재 활성화된 대기열 토큰이 10개 존재한다.

            // when : 현재 활성화된 대기열 토큰의 수를 조회한다.

            // then : 현재 활성화된 대기열 토큰의 수는 10이다.
        }
    }

    @Nested
    class ConcurrentScenario {
        @Test
        @DisplayName("성공 : 동시에 100명 대기열 진입 시 100개의 대기열 토큰 발급에 성공한다.")
        void shouldSuccessIssuing100Tokens_When100PeopleEnterQueue() {
            // given : 스레드 100개 초기화. 각 스레드는 대기열 진입 시도.

            // when : 해당 스레드 invokeAll() 호출 통해 동시에 토큰 발급 시도.

            // then : 100개의 대기열 토큰이 발급된다. 대기 중 토큰 수는 100개.
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
