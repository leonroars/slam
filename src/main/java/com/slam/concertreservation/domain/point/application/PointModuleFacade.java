package com.slam.concertreservation.domain.point.application;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.domain.point.api.PointModuleApi;
import com.slam.concertreservation.domain.point.api.PointOperationResult;
import com.slam.concertreservation.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * 논리적으로 분리된 다른 도메인에서의 포인트 관련 기능을 제공하는 퍼사드 클래스.
 * <br></br>
 * 비즈니스 규칙 위반 예외에 대해 Exponential Backoff 전략으로 재시도를 수행하도록 설정. (100ms -> 200ms -> 400ms)
 * <br></br>
 * 재시도 주기는 현행 설계 상 JVM 내부에서만 요청이 발생한다는 점, Connection Pool 고갈 방지를 위해 짧게 설정.
 */

@Component
@RequiredArgsConstructor
public class PointModuleFacade implements PointModuleApi {

    private final PointService pointService;

    @Override
    @Retryable(
            retryFor = Exception.class,
            noRetryFor = {BusinessRuleViolationException.class, OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public PointOperationResult decreaseUserPointBalance(Long userId, int amount) {
        pointService.decreaseUserPointBalance(userId, amount);
        return PointOperationResult.success(userId, amount);
    }

    @Override
    @Retryable(
            retryFor = Exception.class,
            noRetryFor = {BusinessRuleViolationException.class, OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public PointOperationResult increaseUserPointBalance(Long userId, int amount) {
        pointService.increaseUserPointBalance(userId, amount);
        return PointOperationResult.success(userId, amount);
    }

    @Recover
    public PointOperationResult recoverPointOperation(Exception e, Long userId, int amount) {
        return PointOperationResult.fail(userId, amount, e.getMessage());
    }
}
