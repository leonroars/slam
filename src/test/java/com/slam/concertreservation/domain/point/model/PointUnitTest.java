package com.slam.concertreservation.domain.point.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slam.concertreservation.exceptions.BusinessRuleViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code Point} 도메인 모델에 대한 단위테스트.
 */

class PointUnitTest {

    @Test
    @DisplayName("실패 : 0보다 적은 금액을 가진 Point 객체 인스턴스 생성 시도 시 BusinessRuleViolationException 발생.")
    void shouldThrowBusinessRuleViolationException_WhenAmountBelowZero(){
        // given
        int negativeAmount = -1;

        // when & then
        assertThatThrownBy(() -> Point.create(negativeAmount))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("실패 : 1,000,000 점을 초과하는 금액을 갖는 Point 객체 인스턴스 생성 시도 시 BusinessRuleViolationException 발생")
    void shouldThrowBusinessRuleViolationException_WhenAmountOverMillion(){
        // given
        int overMillion = 1_000_001;

        // when & then
        assertThatThrownBy(() -> Point.create(overMillion))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("성공 : 적법한 금액을 갖는 Point 객체 인스턴스 생성 시도는 성공한다.")
    void shouldSucceed_WhenAmountIsLegal(){
        // given
        int legalAmount = 1000;

        // when & then
        Assertions.assertThatCode(() -> Point.create(legalAmount))
                .doesNotThrowAnyException();
    }

}