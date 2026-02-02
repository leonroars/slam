package com.slam.concertreservation.domain.queue.repository;

import com.slam.concertreservation.domain.queue.model.Token;
import java.util.List;
import java.util.Optional;

public interface TokenRepository {
    // 토큰 생성 : V
    Token save(Token token);

    // 상태 변경한 토큰들 한 번에 저장. : V
    List<Token> saveAll(List<Token> tokens);

    // 토큰 ID로 단건 조회. : V
    Optional<Token> findTokenWithIdAndConcertScheduleId(Long concertScheduleId, String tokenId);

    // 공연 일정 ID로 토큰 전체 조회
    List<Token> findByConcertScheduleId(Long concertScheduleId);

    // 활성화 상태로 변경할 토큰 K 개 조회.
    List<Token> findNextKTokensToBeActivated(Long concertScheduleId, int k);

    // 공연 일정 ID와 상태로 활성화 토큰 수 집계. 이는 곧 현재 서비스 이용 중인 사용자 수를 의미합니다.
    int countCurrentlyActiveTokens(Long concertScheduleId);

    // 공연 일정 ID, 사용자 ID, 상태 목록으로 남은 토큰 수 집계.
    int countRemaining(Long concertScheduleId, String tokenId);

    // 활성화된 토큰 중 만료될 토큰 조회.
    List<Token> findActivatedTokensToBeExpired(Long concertScheduleId);

    // 대기 중인 토큰 중 만료될 토큰 조회.
    List<Token> findWaitingTokensToBeExpired();

    // 공연 일정 ID로 대기 중 토큰 수 집계.
    int countCurrentlyWaitingTokens(Long concertScheduleId);

    void setQueueExpiration(Long concertScheduleId, long ttlSeconds);
}
