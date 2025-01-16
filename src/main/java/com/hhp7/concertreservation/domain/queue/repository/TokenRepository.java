package com.hhp7.concertreservation.domain.queue.repository;

import com.hhp7.concertreservation.domain.queue.model.Token;
import com.hhp7.concertreservation.domain.queue.model.TokenStatus;
import java.util.List;
import java.util.Optional;

public interface TokenRepository {
    // 토큰 생성
    Token save(Token token);

    // 상태 변경한 토큰들 한 번에 저장.
    List<Token> saveAll(List<Token> tokens);

    // 토큰 조회
    Optional<Token> findById(String tokenId);

    // 공연 일정 ID, 사용자 ID, 상태로 토큰 조회
    Optional<Token> findByConcertScheduleIdAndUserIdAndStatus(String concertScheduleId, String userId, TokenStatus status);

    // 공연 일정 ID로 토큰 전체 조회
    List<Token> findByConcertScheduleId(String concertScheduleId);

    // 활성화 상태로 변경할 토큰 K 개 조회.
    List<Token> findNextKTokensByConcertScheduleIdAndStatus(String concertScheduleId, TokenStatus status, int k);

    // 공연 일정 ID와 상태로 토큰 수 집계.
    // TokenStatus 를 고정(ACTIVE)로 받지 않는 이유는, 이후 다른 상태에 대한 토큰 목록 조회가 필요할 수 있다고 판단했기 때문입니다.
    int countTokensByConcertScheduleIdAndStatus(String concertScheduleId, TokenStatus status);

    // 공연 일정 ID, 사용자 ID, 상태 목록으로 남은 토큰 수 집계.
    int countRemainingByConcertScheduleIdAndTokenIdAndStatus(String concertScheduleId, String userId);

    // 만료 토큰 조회 및 반환
    List<Token> findExpiredTokens();

}
