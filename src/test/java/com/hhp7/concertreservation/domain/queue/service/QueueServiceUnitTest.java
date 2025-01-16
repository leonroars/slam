package com.hhp7.concertreservation.domain.queue.service;

import com.hhp7.concertreservation.domain.queue.model.Token;
import com.hhp7.concertreservation.domain.queue.model.TokenStatus;
import com.hhp7.concertreservation.domain.queue.repository.TokenRepository;
import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
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

    private final String userId = "user123";
    private final String concertScheduleId = "concert1";
    private final int maxConcurrentUsers = queueService.MAX_CONCURRENT_USER;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("성공 : 해당 공연 일정 및 사용자가 발급한 토큰 존재하지 않을 경우 성공적으로 토큰이 발급된다.")
    void issueToken_SuccessfullyIssuesToken_WhenNoExistingTokens() {
        // given : 처음 사용자 아이디와 공연 일정 아이디로 조회 시 토큰 존재하지 않는다.
        when(tokenRepository.findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.WAIT))
                .thenReturn(Optional.empty());
        when(tokenRepository.findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // 이후 정상적으로 생성될 토큰
        Token expected = Token.create(userId, concertScheduleId);
        when(tokenRepository.save(any(Token.class))).thenReturn(expected);

        // when
        Token result = queueService.issueToken(userId, concertScheduleId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getConcertScheduleId()).isEqualTo(concertScheduleId);
        assertThat(result.getStatus()).isEqualTo(TokenStatus.WAIT);
        verify(tokenRepository, times(1)).findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.WAIT);
        verify(tokenRepository, times(1)).findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.ACTIVE);
        verify(tokenRepository, times(1)).save(any(Token.class));
    }

    @Test
    @DisplayName("실패 : 해당 공연 일정 및 사용자가 대기 중인 토큰이 존재할 경우 UnavailableRequestException이 발생한다.")
    void shouldThrowsUnavailableRequestException_WhenExistingWaitTokenExists() {
        // given
        Token existingToken = Token.create(userId, concertScheduleId);
        when(tokenRepository.findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.WAIT))
                .thenReturn(Optional.of(existingToken));

        // when & then
        assertThatThrownBy(() -> queueService.issueToken(userId, concertScheduleId))
                .isInstanceOf(UnavailableRequestException.class)
                .hasMessage("이미 대기 중인 토큰이 존재합니다.");
        verify(tokenRepository, times(1)).findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.WAIT);
        verify(tokenRepository, never()).findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.ACTIVE);
        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    @DisplayName("실패 : 해당 공연 일정 및 사용자가 활성화된 토큰이 존재할 경우 UnavailableRequestException이 발생한다.")
    void issueToken_ThrowsException_WhenExistingActiveTokenExists() {
        // Arrange
        Token existingToken = Token.create(userId, concertScheduleId);
        when(tokenRepository.findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.WAIT))
                .thenReturn(Optional.empty());
        when(tokenRepository.findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.ACTIVE))
                .thenReturn(Optional.of(existingToken));

        // Act & Assert
        assertThatThrownBy(() -> queueService.issueToken(userId, concertScheduleId))
                .isInstanceOf(UnavailableRequestException.class)
                .hasMessage("이미 활성화된 토큰이 존재합니다.");

        verify(tokenRepository, times(1)).findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.WAIT);
        verify(tokenRepository, times(1)).findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.ACTIVE);
        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    @DisplayName("성공 : 대기 중인 토큰이 존재할 경우 K명의 토큰을 활성화한다.")
    void shouldActivateTokensSuccessfully_WhenWithinLimit() {
        // given
        int k = 5;
        int activeCount = 30;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(30);

        when(tokenRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE))
                .thenReturn(activeCount);

        List<Token> waitingTokens = List.of(
                Token.create("1", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("2", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("3", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("4", userId, concertScheduleId, createdAt, expiredAt),
                Token.create("5", userId, concertScheduleId, createdAt, expiredAt)
        );

        when(tokenRepository.findNextKTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT, k))
                .thenReturn(waitingTokens);

        List<Token> activatedTokens = List.of(
                Token.create("1", userId, concertScheduleId, createdAt, expiredAt).activate(),
                Token.create("2", userId, concertScheduleId, createdAt, expiredAt).activate(),
                Token.create("3", userId, concertScheduleId, createdAt, expiredAt).activate(),
                Token.create("4", userId, concertScheduleId, createdAt, expiredAt).activate(),
                Token.create("5", userId, concertScheduleId, createdAt, expiredAt).activate()
        );

        when(tokenRepository.saveAll(waitingTokens))
                .thenReturn(activatedTokens);
        when(tokenRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE))
                .thenReturn(maxConcurrentUsers - k);

        // when
        List<Token> result = queueService.activateTokens(concertScheduleId);

        // then
        assertThat(result).hasSize(k);
        assertThat(result).allMatch(token -> token.getStatus() == TokenStatus.ACTIVE);
        verify(tokenRepository, times(1)).countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE);
        verify(tokenRepository, times(1)).findNextKTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT, k);
        verify(tokenRepository, times(1)).saveAll(waitingTokens);
    }

    @Test
    @DisplayName("실패 : 동시 예약 가능한 사용자 수가 0일 때 토큰 발급 시도할 경우 UnavailableRequestException이 발생한다.")
    void shouldThrowsUnavailableRequestException_WhenReachesMaxConcurrentUsers() {
        // given
        int activeCount = 40; // MAX_CONCURRENT_USER (40)

        when(tokenRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE))
                .thenReturn(activeCount);


        // when & then
        assertThatThrownBy(() -> queueService.activateTokens(concertScheduleId))
                .isInstanceOf(UnavailableRequestException.class)
                .hasMessage("최대 동시 예약 가능한 사용자 수를 초과했습니다.");
    }


    @Test
    @DisplayName("실패 : 대기 중인 토큰이 없을 때 토큰 활성화 시도 시 UnavailableRequestException이 발생한다.")
    void shouldThrowUnavailableRequestException_WhenNoWaitingTokensAvailable() {
        // given
        int k = 5;
        int activeCount = 35; // availableSlots = 5

        when(tokenRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE))
                .thenReturn(activeCount);

        when(tokenRepository.findNextKTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT, k))
                .thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> queueService.activateTokens(concertScheduleId))
                .isInstanceOf(UnavailableRequestException.class);

        verify(tokenRepository, times(1)).countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE);
        verify(tokenRepository, times(1)).findNextKTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT, k);
        verify(tokenRepository, never()).saveAll(anyList());
    }


    @Test
    @DisplayName("성공 : 토큰의 상태가 WAIT인 경우 토큰 만료가 성공한다.")
    void shouldSuccessfullyExpiresToken_WhenTokenExistsAndItsStatusIsWAIT() {
        // given
        String tokenId = "token1";
        Token existingToken = Token.create("userId", "concertScheduleId");

        when(tokenRepository.findById(tokenId))
                .thenReturn(Optional.of(existingToken));

        Token expiredToken = Token.create("userId", "concertScheduleId").expire();
        when(tokenRepository.save(existingToken))
                .thenReturn(expiredToken);

        // when
        Token result = queueService.expireToken(tokenId);

        // then
        assertThat(result.getStatus()).isEqualTo(TokenStatus.EXPIRED);
        verify(tokenRepository, times(1)).findById(tokenId);
        verify(tokenRepository, times(1)).save(existingToken);
    }

    @Test
    @DisplayName("실패 : 토큰의 상태가 이미 EXPIRE인 토큰 만료 시도 시 BusinessRuleViolationException 발생한다.")
    void shouldThrowsBusinessRuleViolationException_WhenTokenDoesNotExist() {
        // given
        String tokenId = "token1";
        Token token = Token.create("userId", "concertScheduleId").expire();

        when(tokenRepository.findById(tokenId))
                .thenReturn(Optional.of(token));

        // Act & Assert
        assertThatThrownBy(() -> queueService.expireToken(tokenId))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(tokenRepository, times(1)).findById(tokenId);
        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    @DisplayName("성공 : 특정 토큰 앞 대기 혹은 사용 중인 토큰 수 조회가 이루어진다.")
    void shouldReturnRemainingTokenCountSuccessfully() {
        // given
        String userId = "user123";
        String concertScheduleId = "concert1";
        int expectedCount = 5;

        when(tokenRepository.countRemainingByConcertScheduleIdAndTokenIdAndStatus(concertScheduleId, userId))
                .thenReturn(expectedCount);

        // when
        int result = queueService.getRemainingTokenCount(concertScheduleId, userId);

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(tokenRepository, times(1)).countRemainingByConcertScheduleIdAndTokenIdAndStatus(concertScheduleId, userId);
    }
}