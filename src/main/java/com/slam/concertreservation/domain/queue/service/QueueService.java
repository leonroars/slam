package com.slam.concertreservation.domain.queue.service;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.domain.queue.model.QueuePolicy;
import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.queue.repository.TokenRepository;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final TokenRepository tokenRepository;
    private final QueuePolicy queuePolicy;

    /**
     * 특정 공연 일정 예약을 위해 대기하는 어떤 사용자에게 토큰을 발급합니다.
     * <br>
     * </br>
     * 토큰 발급 시 중복 검증을 시행하지 않습니다. 해당 서비스 로직은 대기열 진입 시에 호출이 되기 때문에, 해당 진입 시점에서 정책 상
     * 새롭게 토큰을 발급해야 하기 때문입니다.
     * <br>
     * </br>
     * 그렇게 하지 않을 경우 '새치기'가 가능합니다. 또한 레퍼런스(인터파크 티켓 예매)에서도 '새로 고침 혹은 재접속 시 순번이 밀려날 수
     * 있음을 고지'하고 있아 마찬가지의 로직으로 구현된 것으로 사료됩니다.
     * <br>
     * </br>
     * 악의적 행위(대기열로 토근 발급 신청을 반복하는 등)에 대한 대응은 통상 비즈니스 로직 밖에서 이루어지는 것도 고려하였습니다.
     * <br>
     * </br>
     * 또한, 현재 동시 예약 이용 중인 사용자 수가 최대 동시 예약 가능한 사용자 수를 초과할 경우 대기 상태로 토큰을 발급하고, 아닐 경우
     * 활성화 상태로 토큰을 발급합니다.
     * 
     * @param userId
     * @param concertScheduleId
     * @return
     */
    public Token issueToken(Long userId, String concertScheduleId) {
        // 토큰 생성
        Token token = Token.create(userId, concertScheduleId, queuePolicy.getWaitingTokenDuration());

        // 현재 예약 서비스 이용 중인 사용자 수 확인
        int activeTokenCount = tokenRepository.countCurrentlyActiveTokens(concertScheduleId);
        int waitingCount = tokenRepository.countCurrentlyWaitingTokens(concertScheduleId);

        // 만약 대기가 필요 없을 경우 바로 활성화 및 만료 시간 설정(5분)
        if (activeTokenCount < queuePolicy.calculateConcurrentUserThreshold()) {
            token.activate(queuePolicy.getActiveTokenDuration());
        }

        // 발급된 토큰 저장.
        Token saved = tokenRepository.save(token);

        // Redis 내 토큰 보관 자료구조 TTL 설정
        tokenRepository.setQueueExpiration(concertScheduleId, Duration.ofHours(12).getSeconds());

        // 로그 기록
        log.info("토큰 발급 완료 - tokenId: {}, userId: {}, status: {}, activeUsers: {}, waitingUsers: {}",
                saved.getId(), userId, saved.getStatus(), activeTokenCount, waitingCount);

        return saved;
    }

    /**
     * 특정 공연 일정 예약을 위해 대기하는 K명의 사용자를 추가로 서비스에 진입시킵니다.
     * 
     * @param concertScheduleId
     * @return
     */
    public List<Token> activateTokens(String concertScheduleId, int k) {
        // 현재 동시 예약 서비스 이용 중인 사용자 수 확인
        int activeTokenCount = tokenRepository.countCurrentlyActiveTokens(concertScheduleId);

        // 현재 동시 예약 서비스 이용 중인 사용자 수 < 정책 한계 -> 빈 리스트 반환(토큰 0개 활성화)
        if (activeTokenCount >= queuePolicy.getMaxConcurrentUser()) {
            return List.of();
        }

        // 대기 중인 토큰 중 K 개 활성화
        List<Token> activated = tokenRepository.findNextKTokensToBeActivated(concertScheduleId, k);

        if (!activated.isEmpty()) {
            activated = activated.stream()
                    .map(token -> token.activate(queuePolicy.getActiveTokenDuration()))
                    .toList();
        }

        List<Token> saved = tokenRepository.saveAll(activated);

        log.info("토큰 활성화 완료 - concertScheduleId: {}, activatedCount: {}, currentActiveUsers: {}",
                concertScheduleId, activated.size(), activeTokenCount + activated.size());

        return saved;
    }

    public Token expireToken(String concertScheduleId, String tokenId) {
        Token token = tokenRepository.findTokenWithIdAndConcertScheduleId(concertScheduleId, tokenId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.TOKEN_NOT_FOUND, "해당 토큰이 존재하지 않습니다."));
        token.expire();

        return tokenRepository.save(token);
    }

    public List<Token> expireToken(String concertScheduleId, List<Token> tokens) {
        List<Token> expiredTokens = tokenRepository.saveAll(
                tokens.stream()
                        .map(Token::expire)
                        .toList());

        log.info("토큰 일괄 만료 처리 완료 - concertScheduleId: {}, expiredCount: {}",
                concertScheduleId, expiredTokens.size());

        return expiredTokens;
    }

    public int getRemainingTokenCount(String concertScheduleId, String tokenId) {
        return tokenRepository.countRemaining(concertScheduleId, tokenId);
    }

    /**
     * 토큰 검증
     * 
     * @param tokenId
     * @return
     */
    public boolean validateToken(String concertScheduleId, String tokenId) {
        return tokenRepository.findTokenWithIdAndConcertScheduleId(concertScheduleId, tokenId)
                .map(token -> {
                    if (token.isExpired()) {
                        throw new UnavailableRequestException(ErrorCode.TOKEN_EXPIRED, "만료된 토큰이므로 사용 불가합니다.");
                    } else if (token.isWait()) {
                        throw new UnavailableRequestException(ErrorCode.INVALID_REQUEST, "대기 중인 토큰은 사용 불가합니다.");
                    } else {
                        return true;
                    }
                })
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.TOKEN_NOT_FOUND, "해당 토큰이 존재하지 않습니다."));
    }

    /**
     * 만료될 활성화 토큰 조회
     * 
     * @return
     */
    public List<Token> getActivatedTokensToBeExpired(String concertScheduleId) {
        return tokenRepository.findActivatedTokensToBeExpired(concertScheduleId);
    }

    /**
     * 만료될 대기 중 토큰 조회
     * 
     * @return
     */
    public List<Token> getWaitingTokensToBeExpired() {
        return tokenRepository.findWaitingTokensToBeExpired();
    }

    /**
     * 특정 공연 일정에 발급된 모든 토큰 조회
     * 
     * @param concertScheduleId
     * @return
     */
    public List<Token> getAllTokensByConcertScheduleId(String concertScheduleId) {
        return tokenRepository.findByConcertScheduleId(concertScheduleId);
    }
}
