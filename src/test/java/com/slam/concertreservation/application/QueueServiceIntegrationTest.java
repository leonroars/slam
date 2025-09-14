package com.slam.concertreservation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.service.ConcertService;
import com.slam.concertreservation.domain.queue.model.QueuePolicy;
import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.queue.model.TokenStatus;
import com.slam.concertreservation.domain.queue.service.QueueService;
import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.domain.user.service.UserService;
import com.slam.concertreservation.exceptions.UnavailableRequestException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class QueueServiceIntegrationTest {

    private final String CONCERT_SCHEDULE_ID = "concertScheduleId";
    private final LocalDateTime CONCERT_SCHEDULE_START_TIME = LocalDateTime.now().plusDays(2);
    private final LocalDateTime CONCERT_SCHEDULE_RESERVATION_START_TIME = LocalDateTime.now().plusDays(1).minusHours(3);
    private final LocalDateTime CONCERT_SCHEDULE_RESERVATION_END_TIME = LocalDateTime.now().plusDays(1).minusHours(1);

    @Autowired
    private QueueService queueService;

    @Autowired
    private QueuePolicy queuePolicy;

    @Autowired
    private ConcertService concertService;

    @Autowired
    private UserService userService;

    @Nested
    class NonConcurrentScenario {

        @Test
        @DisplayName("성공 : 어떤 사용자가 대기열에 진입하면 대기열 토큰이 발급된다.")
        void shouldSuccessIssueToken_WhenEnterQueue() {
            // given : 어떤 사용자와 공연 일정 존재
            User user = User.create("user1");
            ConcertSchedule concertSchedule = ConcertSchedule.create(
                    CONCERT_SCHEDULE_ID,
                    CONCERT_SCHEDULE_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_END_TIME
            );

            assertThat(queuePolicy.getMaxConcurrentUser()).isEqualTo(2);

            User savedUser = userService.joinUser(user);
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            // when : 해당 사용자가 해당 공연 일정 대기열 진입
            Token issuedToken = queueService.issueToken(savedUser.getId(), savedConcertSchedule.getId());

            // then : 발급된 토큰이 존재하고, 첫 번째 토큰이므로 활성화 상태
            assertThat(queueService.getAllTokensByConcertScheduleId(savedConcertSchedule.getId()))
                    .hasSize(1);
            assertThat(issuedToken.getStatus()).isEqualTo(TokenStatus.ACTIVE);
            assertThat(queueService.validateToken(savedConcertSchedule.getId(), issuedToken.getId()))
                    .isTrue();
        }

        @Test
        @DisplayName("성공 : 한 번에 K개의 대기열 토큰 상태를 '활성화'로 변경할 수 있다.")
        void shouldSuccessActivateKTokens_WhenActivateKTokensAtOnce() {
            // given : 정책 상 최대 2명 활성화 가능, 현재 모두 활성화 상태에서 추가 3명 대기
            ConcertSchedule concertSchedule = ConcertSchedule.create(
                    CONCERT_SCHEDULE_ID,
                    CONCERT_SCHEDULE_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_END_TIME
            );
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            // 먼저 2명을 활성화 상태로 (정책 한계까지 채움)
            User activeUser1 = userService.joinUser(User.create("active1"));
            User activeUser2 = userService.joinUser(User.create("active2"));
            Token activeUser1Token = queueService.issueToken(activeUser1.getId(), savedConcertSchedule.getId());
            Token activeUser2Token = queueService.issueToken(activeUser2.getId(), savedConcertSchedule.getId());

            // 대기 상태로 3명 추가
            User waitingUser1 = userService.joinUser(User.create("waiting1"));
            User waitingUser2 = userService.joinUser(User.create("waiting2"));
            User waitingUser3 = userService.joinUser(User.create("waiting3"));

            Token waitingToken1 = queueService.issueToken(waitingUser1.getId(), savedConcertSchedule.getId());
            Token waitingToken2 = queueService.issueToken(waitingUser2.getId(), savedConcertSchedule.getId());
            Token waitingToken3 = queueService.issueToken(waitingUser3.getId(), savedConcertSchedule.getId());

            // 대기 상태 확인
            assertThat(waitingToken1.getStatus()).isEqualTo(TokenStatus.WAIT);
            assertThat(waitingToken2.getStatus()).isEqualTo(TokenStatus.WAIT);
            assertThat(waitingToken3.getStatus()).isEqualTo(TokenStatus.WAIT);

            // 활성화된 토큰 중 일부를 만료시킴 (공간 확보)
            queueService.expireToken(savedConcertSchedule.getId(), activeUser1Token.getId());
            queueService.expireToken(savedConcertSchedule.getId(), activeUser2Token.getId());

            // when : 2개의 대기열 토큰을 활성화
            List<Token> activatedTokens = queueService.activateTokens(savedConcertSchedule.getId(), 2);

            // then : 2개의 토큰이 활성화됨
            assertThat(activatedTokens).hasSize(2);
            assertThat(queueService.validateToken(savedConcertSchedule.getId(), waitingToken1.getId()))
                    .isTrue();
            assertThat(queueService.validateToken(savedConcertSchedule.getId(), waitingToken2.getId()))
                    .isTrue();
        }

        @Test
        @DisplayName("성공 : 어떤 사용자의 대기열 순번을 조회할 수 있다.")
        void shouldSuccessGetQueueNumber_WhenGetQueueNumber() {
            // given : 정책상 최대 2명 활성화, 5명의 사용자
            ConcertSchedule concertSchedule = ConcertSchedule.create(
                    CONCERT_SCHEDULE_ID,
                    CONCERT_SCHEDULE_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_END_TIME
            );
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            List<User> users = List.of(
                    User.create("user1"),
                    User.create("user2"),  // 여기까지 활성화 (정책: max 2명)
                    User.create("user3"),  // 여기부터 대기
                    User.create("user4"),
                    User.create("user5")
            );

            List<Token> tokens = new ArrayList<>();
            for (User user : users) {
                User savedUser = userService.joinUser(user);
                Token token = queueService.issueToken(savedUser.getId(), savedConcertSchedule.getId());
                tokens.add(token);
            }

            // when & then : 토큰 상태 확인
            // 처음 2개는 활성화 (정책 기준)
            assertThat(tokens.get(0).getStatus()).isEqualTo(TokenStatus.ACTIVE);
            assertThat(tokens.get(1).getStatus()).isEqualTo(TokenStatus.ACTIVE);

            // 나머지는 대기
            assertThat(tokens.get(2).getStatus()).isEqualTo(TokenStatus.WAIT);
            assertThat(tokens.get(3).getStatus()).isEqualTo(TokenStatus.WAIT);
            assertThat(tokens.get(4).getStatus()).isEqualTo(TokenStatus.WAIT);

            // 5번째 사용자(user5)의 대기 순번 조회
            Token lastToken = tokens.get(4);
            int queuePosition = queueService.getRemainingTokenCount(
                    savedConcertSchedule.getId(),
                    lastToken.getId()
            );

            // user3, user4가 앞에 있으므로 순번은 2 (0-based)
            assertThat(queuePosition).isEqualTo(2);
        }
    }

    @Nested
    class ConcurrentScenario {

        @Test
        @DisplayName("성공 : 10명이 동시에 대기열에 진입한다. 10개 모두 성공한다.")
        void shouldSuccessIssueAllTokens_When10PeopleEnterQueueConcurrently()
                throws InterruptedException, ExecutionException {
            // given : 10명의 사용자와 공연 일정
            List<User> users = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                User user = userService.joinUser(User.create("user" + i));
                users.add(user);
            }

            ConcertSchedule concertSchedule = ConcertSchedule.create(
                    CONCERT_SCHEDULE_ID,
                    CONCERT_SCHEDULE_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_END_TIME
            );
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            // when : 10명이 동시에 토큰 발급 요청
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            List<Callable<Token>> tasks = new ArrayList<>();

            for (User user : users) {
                tasks.add(() -> queueService.issueToken(user.getId(), savedConcertSchedule.getId()));
            }

            Collections.shuffle(tasks);
            List<Future<Token>> results = executorService.invokeAll(tasks);
            executorService.shutdown();

            // then : 모든 요청이 성공하고, 정책에 따라 상태 분배
            List<Token> issuedTokens = new ArrayList<>();
            for (Future<Token> result : results) {
                Token token = result.get();
                assertThat(token).isNotNull();
                issuedTokens.add(token);
            }

            // 전체 10개 토큰 발급 확인
            assertThat(issuedTokens).hasSize(10);

            assertThat(queueService.getAllTokensByConcertScheduleId(savedConcertSchedule.getId()))
                    .hasSize(10);

            // 정책에 따라 최대 2개만 활성화 (max-concurrent-user: 2, threshold: 1.0)
            long activeCount = issuedTokens.stream()
                    .filter(t -> t.getStatus() == TokenStatus.ACTIVE)
                    .count();
            long waitingCount = issuedTokens.stream()
                    .filter(t -> t.getStatus() == TokenStatus.WAIT)
                    .count();
        }

        @Test
        @DisplayName("성공 : 동시 활성화 시도 시 정책 한계를 초과하지 않는다.")
        void shouldNotExceedPolicyLimit_WhenConcurrentActivation()
                throws InterruptedException, ExecutionException {
            // given : 이미 한계까지 활성화된 상태에서 추가 활성화 시도
            ConcertSchedule concertSchedule = ConcertSchedule.create(
                    CONCERT_SCHEDULE_ID,
                    CONCERT_SCHEDULE_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_START_TIME,
                    CONCERT_SCHEDULE_RESERVATION_END_TIME
            );
            ConcertSchedule savedConcertSchedule = concertService.registerConcertSchedule(concertSchedule, 10000);

            // 정책 한계(2명)까지 활성화
            for (int i = 1; i <= queuePolicy.getMaxConcurrentUser(); i++) {
                User user = userService.joinUser(User.create("active" + i));
                queueService.issueToken(user.getId(), savedConcertSchedule.getId());
            }

            // 5명 추가 대기
            List<User> waitingUsers = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                User user = userService.joinUser(User.create("waiting" + i));
                queueService.issueToken(user.getId(), savedConcertSchedule.getId());
                waitingUsers.add(user);
            }

            // when : 동시에 활성화 시도
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            List<Callable<Boolean>> tasks = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                tasks.add(() -> {
                    try {
                        queueService.activateTokens(savedConcertSchedule.getId(), 1);
                        return true;
                    } catch (UnavailableRequestException e) {
                        return false;
                    }
                });
            }

            List<Future<Boolean>> results = executorService.invokeAll(tasks);
            executorService.shutdown();

            // then : 정책 한계로 인해 활성화 실패
            int successCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successCount++;
            }

            // 이미 한계에 도달했으므로 추가 활성화는 모두 실패
            assertThat(successCount).isEqualTo(0);
        }
    }
}