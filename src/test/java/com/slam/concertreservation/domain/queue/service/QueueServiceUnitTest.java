package com.slam.concertreservation.domain.queue.service;

import com.slam.concertreservation.domain.queue.model.QueuePolicy;
import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.queue.model.TokenStatus;
import com.slam.concertreservation.domain.queue.repository.TokenRepository;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.List;
import java.util.Optional;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueServiceUnitTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private QueueService queueService;

    @Mock
    private QueuePolicy queuePolicy;

    private final String userId = "user123";
    private final String concertScheduleId = "concert1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("성공 : 성공적으로 토큰이 발급된다.")
    void issueToken_SuccessfullyIssuesToken_WhenNoExistingTokens() {
        // given
        when(queuePolicy.getActiveTokenDuration()).thenReturn(1);
        when(queuePolicy.getWaitingTokenDuration()).thenReturn(1);
        when(queuePolicy.getMaxConcurrentUser()).thenReturn(5);
        when(queuePolicy.calculateConcurrentUserThreshold()).thenReturn(5);
        // 이후 정상적으로 생성될 토큰
        Token expected = Token.create(userId, concertScheduleId, queuePolicy.getActiveTokenDuration());
        when(tokenRepository.save(any(Token.class))).thenReturn(expected);

        // when
        Token result = queueService.issueToken(userId, concertScheduleId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getConcertScheduleId()).isEqualTo(concertScheduleId);
        assertThat(result.getStatus()).isEqualTo(TokenStatus.WAIT);
        verify(tokenRepository, times(1)).save(any(Token.class));
    }
    @Test
    @DisplayName("성공 : 대기 중인 토큰이 존재할 경우 K명의 토큰을 활성화한다.")
    void shouldActivateTokensSuccessfully_WhenWithinLimit() {
        // given
        int k = 5;
        int activeCount = 30;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(30);

        when(tokenRepository.countCurrentlyActiveTokens(concertScheduleId))
                .thenReturn(activeCount);

        List<Token> waitingTokens = List.of(
                Token.create("1", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("2", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("3", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("4", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("5", userId, concertScheduleId, createdAt, expiredAt)
        );

        when(tokenRepository.findNextKTokensToBeActivated(concertScheduleId, k))
                .thenReturn(waitingTokens);

        when(queuePolicy.getActiveTokenDuration()).thenReturn(1);
        List<Token> activatedTokens = List.of(
                Token.create("1", userId, concertScheduleId, createdAt, expiredAt).activate(
                        queuePolicy.getActiveTokenDuration()),
                Token.create("2", userId, concertScheduleId, createdAt, expiredAt).activate(
                        queuePolicy.getActiveTokenDuration()),
                Token.create("3", userId, concertScheduleId, createdAt, expiredAt).activate(
                        queuePolicy.getActiveTokenDuration()),
                Token.create("4", userId, concertScheduleId, createdAt, expiredAt).activate(
                        queuePolicy.getActiveTokenDuration()),
                Token.create("5", userId, concertScheduleId, createdAt, expiredAt).activate(
                        queuePolicy.getActiveTokenDuration())
        );

        when(queuePolicy.getMaxConcurrentUser()).thenReturn(40);

        when(tokenRepository.saveAll(waitingTokens))
                .thenReturn(activatedTokens);

        when(tokenRepository.countCurrentlyActiveTokens(concertScheduleId))
                .thenReturn(activeCount + k);

        // when
        List<Token> result = queueService.activateTokens(concertScheduleId, k);

        // then
        assertThat(result).hasSize(k);
        assertThat(result).allMatch(token -> token.getStatus() == TokenStatus.ACTIVE);
        verify(tokenRepository, times(1)).countCurrentlyActiveTokens(concertScheduleId);
        verify(tokenRepository, times(1)).findNextKTokensToBeActivated(concertScheduleId, k);
        verify(tokenRepository, times(1)).saveAll(waitingTokens);
    }

    @Test
    @DisplayName("실패 : 동시 예약 가능한 사용자 수가 0일 때 토큰 발급 시도할 경우 UnavailableRequestException이 발생한다.")
    void shouldThrowsUnavailableRequestException_WhenReachesMaxConcurrentUsers() {
        // given
        int activeCount = 40; // MAX_CONCURRENT_USER (40)

        when(tokenRepository.countCurrentlyActiveTokens(concertScheduleId))
                .thenReturn(activeCount);


        // when & then
        assertThatThrownBy(() -> queueService.activateTokens(concertScheduleId, 1))
                .isInstanceOf(UnavailableRequestException.class)
                .hasMessage("최대 동시 예약 가능한 사용자 수를 초과했습니다.");
    }


    @Test
    @DisplayName("성공 : 토큰의 상태가 WAIT인 경우 토큰 만료가 성공한다.")
    void shouldSuccessfullyExpiresToken_WhenTokenExistsAndItsStatusIsWAIT() {
        // given
        when(queuePolicy.getActiveTokenDuration()).thenReturn(1);

        String tokenId = "token1";
        Token existingToken = Token.create("userId", "concertScheduleId", queuePolicy.getActiveTokenDuration());

        when(tokenRepository.findTokenWithIdAndConcertScheduleId("concertScheduleId", tokenId))
                .thenReturn(Optional.of(existingToken));

        Token expiredToken = Token.create("userId", "concertScheduleId", queuePolicy.getActiveTokenDuration()).expire();
        when(tokenRepository.save(existingToken))
                .thenReturn(expiredToken);

        // when
        Token result = queueService.expireToken("concertScheduleId", tokenId);

        // then
        assertThat(result.getStatus()).isEqualTo(TokenStatus.EXPIRED);
        verify(tokenRepository, times(1)).findTokenWithIdAndConcertScheduleId("concertScheduleId", tokenId);
        verify(tokenRepository, times(1)).save(existingToken);
    }

    @Test
    @DisplayName("성공 : 특정 토큰 앞 대기 혹은 사용 중인 토큰 수 조회가 이루어진다.")
    void shouldReturnRemainingTokenCountSuccessfully() {
        // given
        String userId = "user123";
        String tokenId = "token123";
        String concertScheduleId = "concert1";
        int expectedCount = 5;

        when(tokenRepository.countRemaining(concertScheduleId, tokenId))
                .thenReturn(expectedCount);

        // when
        int result = queueService.getRemainingTokenCount(concertScheduleId, tokenId);

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(tokenRepository, times(1)).countRemaining(concertScheduleId, tokenId);
    }
}