package com.slam.concertreservation.domain.payment.application;

import static org.mockito.Mockito.*;

import com.slam.concertreservation.domain.payment.model.CompensationTxLog;
import com.slam.concertreservation.domain.payment.service.CompensationTxLogService;
import com.slam.concertreservation.domain.point.api.PointModuleApi;
import com.slam.concertreservation.domain.point.api.PointOperationResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompensationTxRetrySchedulerTest {

    @InjectMocks
    private CompensationTxRetryScheduler compensationTxRetryScheduler;

    @Mock
    private CompensationTxLogService compensationTxLogService;

    @Mock
    private PointModuleApi pointModuleApi;

    @Nested
    @DisplayName("retryFailedCompensations 테스트")
    class RetryFailedCompensationsTest {

        @Test
        @DisplayName("양수 금액 보상 성공 시 COMPLETED 처리한다")
        void shouldMarkResolved_WhenPositiveAmountCompensationSucceeds() {
            // given
            CompensationTxLog log = CompensationTxLog.create(1L, 100L, 200L, 5000);

            when(compensationTxLogService.getAllRetriables()).thenReturn(List.of(log));
            when(pointModuleApi.increaseUserPointBalance(1L, 5000))
                    .thenReturn(PointOperationResult.success(1L, 5000));

            // when
            compensationTxRetryScheduler.retryFailedCompensations();

            // then
            verify(pointModuleApi).increaseUserPointBalance(1L, 5000);
            verify(compensationTxLogService).markAsCompleted(log);
        }

        @Test
        @DisplayName("음수 금액 보상 성공 시 COMPLETED 처리한다")
        void shouldMarkResolved_WhenNegativeAmountCompensationSucceeds() {
            // given
            CompensationTxLog log = CompensationTxLog.create(1L, 100L, 200L, -3000);

            when(compensationTxLogService.getAllRetriables()).thenReturn(List.of(log));
            when(pointModuleApi.decreaseUserPointBalance(1L, 3000))
                    .thenReturn(PointOperationResult.success(1L, 3000));

            // when
            compensationTxRetryScheduler.retryFailedCompensations();

            // then
            verify(pointModuleApi).decreaseUserPointBalance(1L, 3000);
            verify(compensationTxLogService).markAsCompleted(log);
        }

        @Test
        @DisplayName("보상 실패 시 retryCount를 증가시킨다")
        void shouldIncrementRetryCount_WhenCompensationFails() {
            // given
            CompensationTxLog log = CompensationTxLog.create(1L, 100L, 200L, 5000);

            when(compensationTxLogService.getAllRetriables()).thenReturn(List.of(log));
            when(pointModuleApi.increaseUserPointBalance(1L, 5000))
                    .thenReturn(PointOperationResult.fail(1L, 5000, "포인트 서비스 장애"));

            // when
            compensationTxRetryScheduler.retryFailedCompensations();

            // then
            verify(compensationTxLogService).markAsFailed(log);
        }

        @Test
        @DisplayName("재시도 대상이 없으면 아무 작업도 하지 않는다")
        void shouldDoNothing_WhenNoRetryTargets() {
            // given
            when(compensationTxLogService.getAllRetriables()).thenReturn(List.of());

            // when
            compensationTxRetryScheduler.retryFailedCompensations();

            // then
            verifyNoInteractions(pointModuleApi);
        }

        @Test
        @DisplayName("여러 건 중 일부만 성공해도 각각 독립적으로 처리한다")
        void shouldProcessEachLogIndependently() {
            // given
            CompensationTxLog successLog = CompensationTxLog.create(1L, 100L, 200L, 5000);
            CompensationTxLog failLog = CompensationTxLog.create(2L, 101L, 201L, 3000);

            when(compensationTxLogService.getAllRetriables()).thenReturn(List.of(successLog, failLog));
            when(pointModuleApi.increaseUserPointBalance(1L, 5000))
                    .thenReturn(PointOperationResult.success(1L, 5000));
            when(pointModuleApi.increaseUserPointBalance(2L, 3000))
                    .thenReturn(PointOperationResult.fail(2L, 3000, "장애"));

            // when
            compensationTxRetryScheduler.retryFailedCompensations();

            // then
            verify(compensationTxLogService).markAsCompleted(successLog);
            verify(compensationTxLogService).markAsFailed(failLog);
        }
    }
}