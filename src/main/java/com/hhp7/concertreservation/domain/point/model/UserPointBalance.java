package com.hhp7.concertreservation.domain.point.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;

/**
 * '사용자의 잔액'을 표현하는 도메인 모델입니다.
 * 실제 포인트 감액/증액에 대한 비즈니스 로직에 대한 책임을 가집니다.
 * @param userId
 * @param balance
 */
public record UserPointBalance(
        String userId,
        Point balance
) {
    public static UserPointBalance create(String userId, Point balance){
        return new UserPointBalance(userId, balance); // Point 객체 생성 시 해당 포인트가 비즈니스 정책을 위반할 경우 Point 생성 시점에 예외가 발생합니다.
    }

    /**
     * 비즈니스 정책을 위반하지 않는 경우, 사용자의 포인트 잔액을 증가시킵니다.
     *
     * @param increaseAmount 더해질 포인트 양. 해당 양은 0 이상이어야 하며, 이를 기존 잔액과 더했을 때 총합은 1,000,000점 이하여야 한다.
     * @return (기존 보유량 + 인자로 주어진 추가량) 을 잔액으로 갖는 새로운 {@code UserPointBalance} 객체 인스턴스를 생성하여 반환한다.
     * @throws BusinessRuleViolationException 충전하고자 하는 양이 0 미만이거나 기존 잔액과 합이 1,000,000 점 초과하는 경우
     */
    public UserPointBalance increase(int increaseAmount){
        // Fail : 0보다 작은 충전 금액 충전 시도
        if(increaseAmount < 0){
            throw new BusinessRuleViolationException("충전하고자 하는 포인트는 0보다 커야 합니다.");
        }

        int newAmount = this.balance().getAmount() + increaseAmount;

        // Fail : 기존 잔액과 합산 시 최대 한도 초과하는 금액 충전 시도
        if(newAmount > 1_000_000){
            throw new BusinessRuleViolationException("잔액 최대 보유 한도는 1,000,000점입니다. 초과하여 충전이 불가합니다.");
        }
        // Success : 합산된 잔액을 갖는 새로운 UserPointBalance 객체 인스턴스 생성 후 반환.
        return UserPointBalance.create(this.userId(), Point.create(newAmount));
    }


    /**
     * 비즈니스 정책을 위반하지 않는 경우, 사용자의 포인트 잔액을 차감시킵니다.
     *
     * @param decreaseAmount 차감할 포인트 양.
     *                       차감량은 0보다 작아선 안되며, 차감 시 잔액이 0보다 작아져서도 안된다.
     * @return 차감된 금액을 잔액으로 갖는 새로운 {@code Point} 객체 인스턴스를 생성하여 반환한다.
     * @throws IllegalArgumentException 차감할 포인트 양이 0보다 작거나, 차감 시 잔액이 0보다 작아지는 경우 해당 예외 발생.
     */
    public UserPointBalance decrease(int decreaseAmount){
        if(decreaseAmount < 0){
            throw new IllegalArgumentException("차감하고자 하는 포인트는 0보다 커야합니다.");
        }

        else if(decreaseAmount > 1_000_000){
            throw new IllegalArgumentException("차감 시 보유 잔액이 0원 미만이 되므로 해당 차감은 불가합니다.");
        }

        int newAmount = this.balance().getAmount() - decreaseAmount;
        return UserPointBalance.create(this.userId(), Point.create(newAmount));
    }
}
