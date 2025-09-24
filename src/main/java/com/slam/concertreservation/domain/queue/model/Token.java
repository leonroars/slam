package com.slam.concertreservation.domain.queue.model;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Token {
    private String id;
    private String userId;
    private String concertScheduleId;
    private TokenStatus status; // WAIT, ACTIVE, EXPIRED

    // Domain Model 상에서는 Nullable 한 필드
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

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

    public static Token create(String id, String userId, String concertScheduleId, String status, LocalDateTime createdAt, LocalDateTime expiredAt){
        Token token = new Token();
        token.id = id;
        token.userId = userId;
        token.concertScheduleId = concertScheduleId;
        token.status = TokenStatus.valueOf(status);
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
    public static Token create(String userId, String concertScheduleId, int waitingTokenDurationInHours){
        if(waitingTokenDurationInHours <= 0){throw new BusinessRuleViolationException("대기 토큰의 유효 시간은 0시간보다 커야 합니다.");}
        return Token.create(
                null,
                userId,
                concertScheduleId,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(waitingTokenDurationInHours));
    }

    /**
     * 단일 토큰 활성화. 활성화 시점 기준 5분 후로 만료 시간 초기화.
     * @return
     */
    public Token activate(int durationInMinutes){
        if(durationInMinutes <= 0){throw new BusinessRuleViolationException("활성 토큰의 유효 시간은 0분보다 커야 합니다.");}
        this.status = TokenStatus.ACTIVE;
        this.initiateExpiredAt(LocalDateTime.now().plusMinutes(durationInMinutes));
        return this;
    }

    public Token expire(){
        this.status = TokenStatus.EXPIRED;
        this.initiateExpiredAt(LocalDateTime.now());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token)) return false;
        Token token = (Token) o;
        return Objects.equals(id, token.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
