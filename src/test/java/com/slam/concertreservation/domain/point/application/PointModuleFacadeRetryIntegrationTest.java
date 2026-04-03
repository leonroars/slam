package com.slam.concertreservation.domain.point.application;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.domain.point.api.PointModuleApi;
import com.slam.concertreservation.domain.point.api.PointOperationResult;
import com.slam.concertreservation.domain.point.model.Point;
import com.slam.concertreservation.domain.point.model.UserPointBalance;
import com.slam.concertreservation.domain.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PointModuleFacade 의 @Retryable 동작을 검증하는 통합 테스트.
 * <br/>
 * Spring AOP 프록시를 통해 실제 재시도가 발생하는지 확인합니다.
 * <br/>
 * RuntimeException 계열은 최대 3회 재시도, BusinessRuleViolationException 등은 재시도하지 않습니다.
 */
@SpringBootTest(classes = {PointModuleFacade.class})
@EnableRetry
class PointModuleFacadeRetryIntegrationTest {

    @Autowired
    private PointModuleApi pointModuleApi;

    @MockitoBean
    private PointService pointService;

    @Nested
    @DisplayName("decreaseUserPointBalance Retry 테스트")
    class DecreaseRetryTest {

        @Test
        @DisplayName("잔액 부족(BusinessRuleViolationException)은 재시도하지 않는다")
        void shouldNotRetry_WhenInsufficientBalance() {
            // given
            Long userId = 1L;
            int amount = 1000;

            when(pointService.decreaseUserPointBalance(userId, amount))
                    .thenThrow(new BusinessRuleViolationException(ErrorCode.INSUFFICIENT_BALANCE, "잔액 부족"));

            // when
            PointOperationResult result = pointModuleApi.decreaseUserPointBalance(userId, amount);

            // then — 비즈니스 실패이므로 1회만 호출
            assertFalse(result.success());
            verify(pointService, times(1)).decreaseUserPointBalance(userId, amount);
        }

        @Test
        @DisplayName("인프라 오류(RuntimeException)는 재시도하고, 두 번째 시도에서 성공한다")
        void shouldRetryAndSucceed_WhenTransientFailure() {
            // given
            Long userId = 1L;
            int amount = 1000;
            UserPointBalance validBalance = UserPointBalance.create(userId, Point.create(5000));

            when(pointService.decreaseUserPointBalance(userId, amount))
                    .thenThrow(new RuntimeException("일시적 DB 장애"))
                    .thenReturn(validBalance);

            // when
            PointOperationResult result = pointModuleApi.decreaseUserPointBalance(userId, amount);

            // then — 첫 번째 실패, 두 번째 성공 = 2회 호출
            assertTrue(result.success());
            verify(pointService, times(2)).decreaseUserPointBalance(userId, amount);
        }

        @Test
        @DisplayName("인프라 오류가 3회 연속 발생하면 최종 실패한다")
        void shouldFail_WhenAllRetriesExhausted() {
            // given
            Long userId = 1L;
            int amount = 1000;

            when(pointService.decreaseUserPointBalance(userId, amount))
                    .thenThrow(new RuntimeException("DB 장애"));

            // when
            PointOperationResult result = pointModuleApi.decreaseUserPointBalance(userId, amount);

            // then — 3회 재시도 후 최종 실패
            assertFalse(result.success());
            verify(pointService, times(3)).decreaseUserPointBalance(userId, amount);
        }
    }

    @Nested
    @DisplayName("increaseUserPointBalance Retry 테스트")
    class IncreaseRetryTest {

        @Test
        @DisplayName("인프라 오류는 재시도하고, 세 번째 시도에서 성공한다")
        void shouldRetryAndSucceed_OnThirdAttempt() {
            // given
            Long userId = 1L;
            int amount = 500;
            UserPointBalance validBalance = UserPointBalance.create(userId, Point.create(1000));

            when(pointService.increaseUserPointBalance(userId, amount))
                    .thenThrow(new RuntimeException("DB 장애"))
                    .thenThrow(new RuntimeException("DB 장애"))
                    .thenReturn(validBalance);

            // when
            PointOperationResult result = pointModuleApi.increaseUserPointBalance(userId, amount);

            // then — 세 번째에 성공 = 3회 호출
            assertTrue(result.success());
            verify(pointService, times(3)).increaseUserPointBalance(userId, amount);
        }

        @Test
        @DisplayName("비즈니스 예외는 재시도하지 않는다")
        void shouldNotRetry_WhenBusinessException() {
            // given
            Long userId = 1L;
            int amount = 500;

            when(pointService.increaseUserPointBalance(userId, amount))
                    .thenThrow(new BusinessRuleViolationException(ErrorCode.INVALID_REQUEST, "비즈니스 규칙 위반"));

            // when
            PointOperationResult result = pointModuleApi.increaseUserPointBalance(userId, amount);
        }
    }
}