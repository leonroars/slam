package com.slam.concertreservation.domain.point.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code PointBalance} 도메인 모델에 대한 단위 테스트
 */
public class UserPointBalanceUnitTest {

    @Test
    @DisplayName("실패 : 0보다 적은 금액 차감 시도할 경우 BusinessRuleViolationException 발생하며 실패")
    void shouldThrowBusinessRuleViolationException_WhenNegativeAmountDecrease() {
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create(1L, point);
        int negativeAmount = -1;

        // when & then
        Assertions.assertThatThrownBy(() -> userPointBalance.decrease(negativeAmount))
                .isInstanceOf(BusinessRuleViolationException.class);

    }

    @Test
    @DisplayName("실패 : 1_000_000 초과 금액 차감 시도 시 BusinessRuleViolationException 발생하며 실패.")
    void shouldThrowBusinessRuleViolationException_WhenDecreaseAmountExceedsMillion() {
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create(1L, point);
        int overMillion = 1_000_001;

        // when & then
        Assertions.assertThatThrownBy(() -> userPointBalance.decrease(overMillion))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("실패 : 0 보다 적은 금액 증액 시도할 경우 BusinessRuleViolationException 발생하며 실패")
    void shouldThrowBusinessRuleViolationException_WhenNegativeAmountIncrease() {
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create(1L, point);
        int negativeAmount = -1;

        // when & then
        Assertions.assertThatThrownBy(() -> userPointBalance.increase(negativeAmount))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("실패 : 1,000,000 보다 큰 금액 증액 시도할 경우 BusinessRuleViolationException 발생하며 실패")
    void shouldThrowBusinessRuleViolationException_WhenIncreaseAmountExceedsMillion() {
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create(1L, point);
        int overMillion = 1_000_001;

        // when & then
        Assertions.assertThatThrownBy(() -> userPointBalance.increase(overMillion))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 적법한 금액 차감 시도 시 성공")
    void shouldSuccessfullyDecreaseAmount_WhenProperAmountDecrease() {
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create(1L, point);
        int decreaseAmount = 1;
        UserPointBalance expectedUserPointBalance = UserPointBalance.create(1L,
                Point.create(point.getAmount() - decreaseAmount));

        // when
        UserPointBalance actualUserPointBalance = userPointBalance.decrease(decreaseAmount);

        // then
        assertEquals(expectedUserPointBalance, actualUserPointBalance);
    }

    @Test
    @DisplayName("성공 : 적법한 금액 증액 시도 시 성공")
    void shouldSuccessfullyIncreaseAmount_WhenProperAmountIncrease() {
        // given
        Point point = Point.create(1000);
        UserPointBalance userPointBalance = UserPointBalance.create(1L, point);
        int increaseAmount = 1;

        UserPointBalance expectedUserPointBalance = UserPointBalance.create(1L,
                Point.create(point.getAmount() + increaseAmount));

        // when
        UserPointBalance actualUserPointBalance = userPointBalance.increase(increaseAmount);

        // then
        assertEquals(expectedUserPointBalance, actualUserPointBalance);
    }

}
