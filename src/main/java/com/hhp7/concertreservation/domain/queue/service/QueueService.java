package com.hhp7.concertreservation.domain.queue.service;

import com.hhp7.concertreservation.domain.queue.model.Token;
import com.hhp7.concertreservation.domain.queue.model.TokenStatus;
import com.hhp7.concertreservation.domain.queue.repository.TokenRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final TokenRepository tokenRepository;
    public static final int MAX_CONCURRENT_USER = 40;

    /**
     * 특정 공연 일정 예약을 위해 대기하는 어떤 사용자에게 토큰을 발급합니다.
     * <br></br>
     * 토큰 발급 시 중복 검증을 시행합니다. 중복 시 {@code UnavailableRequestException} 발생합니다.
     * <br></br>
     * 또한, 현재 동시 예약 이용 중인 사용자 수가 최대 동시 예약 가능한 사용자 수를 초과할 경우 대기 상태로 토큰을 발급하고, 아닐 경우 활성화 상태로 토큰을 발급합니다.
     * @param userId
     * @param concertScheduleId
     * @return
     */
    public Token issueToken(String userId, String concertScheduleId){
        // Check existing tokens
        tokenRepository.findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.WAIT)
                .ifPresent(token -> {
                    throw new UnavailableRequestException("이미 대기 중인 토큰이 존재합니다.");
                });

        tokenRepository.findByConcertScheduleIdAndUserIdAndStatus(concertScheduleId, userId, TokenStatus.ACTIVE)
                .ifPresent(token -> {
                    throw new UnavailableRequestException("이미 활성화된 토큰이 존재합니다.");
                });

        // 토큰 생성
        Token token = Token.create(userId, concertScheduleId);

        // 현재 예약 서비스 이용 중인 사용자 수 확인
        int activeTokenCount = tokenRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE);

        // 만약 대기가 필요 없을 경우 바로 활성화.
        if(activeTokenCount < MAX_CONCURRENT_USER){
            token.activate();
        }

        return tokenRepository.save(token);
    }

    /**
     * 특정 공연 일정 예약을 위해 대기하는 K명의 사용자를 추가로 서비스에 진입시킵니다.
     * @param concertScheduleId
     * @return
     */
    public List<Token> activateTokens(String concertScheduleId){
        // 현재 동시 예약 서비스 이용 중인 사용자 수 확인
        int activeTokenCount = tokenRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE);
        if(activeTokenCount >= MAX_CONCURRENT_USER){
            throw new UnavailableRequestException("최대 동시 예약 가능한 사용자 수를 초과했습니다.");
        }

        int nextK = MAX_CONCURRENT_USER - activeTokenCount;

        // 대기 중인 토큰 중 K 개 활성화
        List<Token> activated = tokenRepository.findNextKTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT, nextK)
                .stream()
                .map(Token::activate)
                .toList();
        if(activated.isEmpty()){throw new UnavailableRequestException("대기 중인 토큰이 존재하지 않습니다.");}

        return tokenRepository.saveAll(activated);
    }

    public Token expireToken(String tokenId){
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new UnavailableRequestException("해당 토큰이 존재하지 않습니다."));
        token.expire();

        return tokenRepository.save(token);
    }

    public List<Token> expireToken(List<Token> tokens){
        return tokenRepository.saveAll(tokens.stream().map(Token::expire).toList());
    }

    public int getRemainingTokenCount(String concertScheduleId, String userId){
        return tokenRepository.countRemainingByConcertScheduleIdAndTokenIdAndStatus(concertScheduleId, userId);
    }

    /**
     * 토큰 검증
     * @param tokenId
     * @return
     */
    public boolean validateToken(String tokenId) {
        return tokenRepository.findById(tokenId)
                .map(token -> {
                    if (token.isExpired()) {
                        throw new UnavailableRequestException("만료된 토큰이므로 사용 불가합니다.");
                    } else if (token.isWait()) {
                        throw new UnavailableRequestException("대기 중인 토큰은 사용 불가합니다.");
                    } else {
                        return true;
                    }
                })
                .orElseThrow(() -> new UnavailableRequestException("해당 토큰이 존재하지 않습니다."));
    }

    public List<Token> getExpiredTokens(){
        return tokenRepository.findExpiredTokens();
    }

}
