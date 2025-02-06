package com.hhp7.concertreservation.domain.queue.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class Token {
    private String id; // DBMS 에서 자동 생성해주는 ID 를 사용하므로 Nullable 합니다.
    private String userId;
    private String concertScheduleId;
    private TokenStatus status; // WAIT, ACTIVE, EXPIRED

    // Domain Model 상에서는 Nullable 한 필드
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

    private static final int WAITING_TOKEN_DURATION = 6; // 대기 상태 토큰의 유효시간
    private static final int ACTIVE_TOKEN_DURATION = 3; // 활성 상태 토큰의 유효시간

    private Token(){}

    public static Token create(String id, String userId, String concertScheduleId, LocalDateTime createdAt, LocalDateTime expiredAt){
        Token token = new Token();
        token.id = id;
        token.userId = userId;
        token.concertScheduleId = concertScheduleId;
        token.status = TokenStatus.WAIT;
        token.createdAt = createdAt;
        token.expiredAt = expiredAt;

        return token;
    }

    /**
     * 토큰 생성. 토큰은 생성 직후 저장되었다가 발급되므로, 생성 시점에 만료 시간을 설정합니다.
     * @param userId
     * @param concertScheduleId
     * @return
     */
    public static Token create(String userId, String concertScheduleId){
        return Token.create(
                null,
                userId,
                concertScheduleId,
                null,
                LocalDateTime.now().plusHours(WAITING_TOKEN_DURATION));
    }

    /**
     * 단일 토큰 활성화. 활성화 시점 기준 5분 후로 만료 시간 초기화.
     * @return
     */
    public Token activate(){
        if(this.status == TokenStatus.ACTIVE){
            throw new BusinessRuleViolationException("이미 활성화된 토큰입니다.");
        }
        this.status = TokenStatus.ACTIVE;
        this.initiateExpiredAt(LocalDateTime.now().plusMinutes(ACTIVE_TOKEN_DURATION));
        return this;
    }

    public Token expire(){
        if(this.status == TokenStatus.EXPIRED){
            throw new BusinessRuleViolationException("이미 만료된 토큰입니다.");
        }
        this.status = TokenStatus.EXPIRED;
        return this;
    }

    /**
     * 만료 시간 설정
     * @param expiredAt
     */
    public void initiateExpiredAt(LocalDateTime expiredAt){
        this.expiredAt = expiredAt;
    }

    /**
     * ID 할당 메서드.
     * @param id
     */
    public void assignId(String id){
        this.id = id;
    }

    public boolean isExpired(){
        return this.status == TokenStatus.EXPIRED;
    }

    public boolean isActive(){
        return this.status == TokenStatus.ACTIVE;
    }

    public boolean isWait(){
        return this.status == TokenStatus.WAIT;
    }
}
