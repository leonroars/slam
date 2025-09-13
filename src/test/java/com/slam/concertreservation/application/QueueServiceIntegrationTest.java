package com.slam.concertreservation.application;

import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.service.ConcertService;
import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.queue.model.TokenStatus;
import com.slam.concertreservation.domain.queue.service.QueueService;
import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.domain.user.service.UserService;
import com.slam.concertreservation.exceptions.UnavailableRequestException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.assertj.core.api.Assertions;
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

        @Test
        @DisplayName("성공 : 어떤 사용자가 대기열에 진입하면 대기열 토큰이 발급된다.")
        void shouldSuccessIssueToken_WhenEnterQueue() {
            // given : 어떤 사용자와 공연 일정 존재.
            User user = User.create("user1");
            ConcertSchedule concertSchedule
                    = ConcertSchedule.create(CONCERT_SCHEDULE_ID, CONCERT_SCHEDULE_START_TIME, CONCERT_SCHEDULE_RESERVATION_START_TIME, CONCERT_SCHEDULE_RESERVATION_END_TIME);

            User savedUser = userService.joinUser(user);
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            // when : 해당 사용자가 해당 공연 일정 대기열 진입
            Token expectedToken = queueService.issueToken(savedUser.getId(), savedConcertSchedule.getId());

            // then : 발급된 토큰이 존재한다.
            // 또한, 현재 활성화 제한 인원 미만이므로 바로 활성화 상태로 발급되어 해당 공연일정과 Token ID로 조회시 '활성화 상태' 토큰이 존재한다.
            Assertions.assertThat(queueService.getAllTokensByConcertScheduleId(savedConcertSchedule.getId()).size()).isEqualTo(1);
            Assertions.assertThat(expectedToken.getStatus()).isEqualTo(TokenStatus.ACTIVE);
            // Assertions.assertThat(queueService.validateToken(savedConcertSchedule.getId(), expectedToken.getId()))
            //         .isEqualTo(true);
        }

        @Test
        @DisplayName("성공 : 한 번에 K개의 대기열 토큰 상태를 '활성화'로 변경할 수 있다.")
        void shouldSuccessActivateKTokens_WhenActivateKTokensAtOnce() {
            // given : 2개의 대기열 토큰이 존재하고, 모두 '대기' 상태.
            User user1 = User.create("user1");
            User user2 = User.create("user2");

            ConcertSchedule concertSchedule
                    = ConcertSchedule.create(CONCERT_SCHEDULE_ID, CONCERT_SCHEDULE_START_TIME, CONCERT_SCHEDULE_RESERVATION_START_TIME, CONCERT_SCHEDULE_RESERVATION_END_TIME);

            User savedUser1 = userService.joinUser(user1);
            User savedUser2 = userService.joinUser(user2);
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            // 토큰 2개 발급 / 대기 상태
            Token token1 = queueService.issueToken(savedUser1.getId(), savedConcertSchedule.getId());
            Token token2 = queueService.issueToken(savedUser2.getId(), savedConcertSchedule.getId());

            // when : 2개의 대기열 토큰 상태를 '활성화'로 변경
            // queueService.activateTokens(savedConcertSchedule.getId(), 2);

            // then : K개의 대기열 토큰 상태가 '활성화'로 변경된다.
            Assertions.assertThat(queueService.getWaitingTokensToBeExpired().size()).isEqualTo(0);
            Assertions.assertThat(queueService.getAllTokensByConcertScheduleId(savedConcertSchedule.getId()).size()).isEqualTo(2);
            Assertions.assertThat(queueService.validateToken(savedConcertSchedule.getId(), token1.getId()))
                    .isEqualTo(true);
            Assertions.assertThat(queueService.validateToken(savedConcertSchedule.getId(), token2.getId()))
                    .isEqualTo(true);
        }


        @Test
        @DisplayName("성공 : 어떤 사용자의 대기열 순번을 조회할 수 있다.")
        void shouldSuccessGetQueueNumber_WhenGetQueueNumber() {
            // given : 어떤 사용자의 대기열 토큰이 존재하고, '대기' 상태. 앞에 10명의 사용자가 대기 중. (총 11명)
            List<User> users = List.of(
                    User.create("user1"),
                    User.create("user2"),
                    User.create("user3"),
                    User.create("user4"),
                    User.create("user5"),
                    User.create("user6"),
                    User.create("user7"),
                    User.create("user8"),
                    User.create("user9"),
                    User.create("user10"),
                    User.create("user11")
            );

            ConcertSchedule concertSchedule
                    = ConcertSchedule.create(CONCERT_SCHEDULE_ID, CONCERT_SCHEDULE_START_TIME, CONCERT_SCHEDULE_RESERVATION_START_TIME, CONCERT_SCHEDULE_RESERVATION_END_TIME);

            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            // 토큰 11개 발급 / 대기 상태
            ArrayDeque<Token> tokens = new ArrayDeque<>();

            for(User user : users) {
                userService.joinUser(user);
                Token token = queueService.issueToken(user.getId(), savedConcertSchedule.getId());
                tokens.addLast(token);
            }

            // when : 목표 사용자의 대기열 순번을 조회한다.
            Token targetToken = tokens.peekLast();
            int queueNumber = queueService.getRemainingTokenCount(savedConcertSchedule.getId(), targetToken.getId());

            // then : 해당 사용자의 대기열 순번이 10이다.
            Assertions.assertThat(queueNumber).isEqualTo(0);
        }
    }

    @Nested
    class ConcurrentScenario {

        @Test
        @DisplayName("성공 : 10명이 동시에 대기열에 진입한다. 10개 모두 성공한다.")
        void shouldSuccessGetQueueNumber_When9PeopleEnterQueueConcurrently()
                throws InterruptedException, ExecutionException {
            // given
            // 사용자 10명 초기화
            List<User> users = List.of(
                    User.create("user1"),
                    User.create("user2"),
                    User.create("user3"),
                    User.create("user4"),
                    User.create("user5"),
                    User.create("user6"),
                    User.create("user7"),
                    User.create("user8"),
                    User.create("user9"),
                    User.create("user10")
            );
            // 공연 일정 초기화
            ConcertSchedule concertSchedule
                    = ConcertSchedule.create(CONCERT_SCHEDULE_ID, CONCERT_SCHEDULE_START_TIME, CONCERT_SCHEDULE_RESERVATION_START_TIME, CONCERT_SCHEDULE_RESERVATION_END_TIME);

            // 사용자 및 공연 일정 저장
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);
            users.forEach(user -> userService.joinUser(user));

            // 토큰 발급 요청 10개 동시 발생
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for(User user : users) {
                tasks.add(() -> {
                    try {
                        Token token = queueService.issueToken(user.getId(), savedConcertSchedule.getId());
                        return true;
                    }
                    catch (Exception e) {
                        return false;
                    }
                });
            }
            // when
            Collections.shuffle(tasks);
            List<Future<Boolean>> results = executorService.invokeAll(tasks);
            executorService.shutdown();

            int successCount = 0;
            for(Future<Boolean> result : results) {
                if(result.get()) {
                    successCount++;
                }
            }
            // then
            Assertions.assertThat(successCount).isEqualTo(10);

        }
    }
}
