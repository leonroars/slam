package com.hhp7.concertreservation.domain.point.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code PointBalance} 도메인 모델에 대한 단위 테스트
 */
public class PointBalanceTest {

    @Test
    @DisplayName("실패 : 0보다 적은 금액 차감 시도할 경우 BusinessRuleViolationException 발생하며 실패")
    void shouldThrowBusinessRuleViolationException_WhenNegativeAmountDecrease(){
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create("1",point);
        int negativeAmount = -1;

        // when & then
        Assertions.assertThatThrownBy(() -> userPointBalance.decrease(negativeAmount))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    @DisplayName("실패 : 1_000_000 초과 금액 차감 시도 시 BusinessRuleViolationException 발생하며 실패.")
    void shouldThrowBusinessRuleViolationException_WhenDecreaseAmountExceedsMillion(){
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create("1",point);
        int overMillion = 1_000_001;

        // when & then
        Assertions.assertThatThrownBy(() -> userPointBalance.decrease(overMillion))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
