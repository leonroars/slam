package com.slam.concertreservation.domain.payment.service;

import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import com.slam.concertreservation.domain.payment.model.CompensationTxLog;
import com.slam.concertreservation.domain.payment.model.CompensationTxStatus;
import com.slam.concertreservation.domain.payment.repository.CompensationTxLogRepository;
import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompensationTxLogServiceUnitTest {

    @Mock
    private CompensationTxLogRepository compensationTxLogRepository;

    @InjectMocks
    private CompensationTxLogService compensationTxLogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("log 메서드 테스트")
    class LogTest {

        @Test
        @DisplayName("성공 : 보상 트랜잭션 로그를 생성하면 PENDING 상태의 로그가 저장되고 반환된다.")
        void shouldCreateAndSaveCompensationTxLog() {
            // given
            Long userId = 1L;
            Long reservationId = 1L;
            Long paymentId = 1L;
            int price = 1000;
            CompensationTxLog expected = CompensationTxLog.create(userId, reservationId, paymentId, price);

            when(compensationTxLogRepository.save(any(CompensationTxLog.class))).thenReturn(expected);

            // when
            CompensationTxLog actual = compensationTxLogService.log(userId, reservationId, paymentId, price);

            // then
            verify(compensationTxLogRepository, times(1)).save(any(CompensationTxLog.class));
            assertEquals(CompensationTxStatus.PENDING, actual.getStatus());
            assertEquals(userId, actual.getUserId());
            assertEquals(price, actual.getPrice());
        }
    }

    @Nested
    @DisplayName("markAsCompleted 메서드 테스트")
    class MarkAsCompletedTest {

        @Test
        @DisplayName("성공 : 보상 트랜잭션 로그를 완료 처리하면 COMPLETED 상태로 변경된다.")
        void shouldMarkLogAsCompleted() {
            // given
            CompensationTxLog txLog = CompensationTxLog.create(1L, 1L, 1L, 1000);

            when(compensationTxLogRepository.save(any(CompensationTxLog.class))).thenReturn(txLog);

            // when
            CompensationTxLog actual = compensationTxLogService.markAsCompleted(txLog);

            // then
            verify(compensationTxLogRepository, times(1)).save(txLog);
            assertEquals(CompensationTxStatus.COMPLETED, actual.getStatus());
        }
    }

    @Nested
    @DisplayName("markAsFailed 메서드 테스트")
    class MarkAsFailedTest {

        @Test
        @DisplayName("성공 : 보상 트랜잭션 로그를 실패 처리하면 FAILED 상태로 변경되고 retryCount가 증가한다.")
        void shouldMarkLogAsFailedAndIncrementRetryCount() {
            // given
            CompensationTxLog txLog = CompensationTxLog.create(1L, 1L, 1L, 1000);

            when(compensationTxLogRepository.save(any(CompensationTxLog.class))).thenReturn(txLog);

            // when
            CompensationTxLog actual = compensationTxLogService.markAsFailed(txLog);

            // then
            verify(compensationTxLogRepository, times(1)).save(txLog);
            assertEquals(CompensationTxStatus.FAILED, actual.getStatus());
            assertEquals(1, actual.getRetryCount());
        }

        @Test
        @DisplayName("실패 : 이미 완료 처리된 보상 트랜잭션을 실패 처리하면 BusinessRuleViolationException이 발생한다.")
        void shouldThrowException_WhenAlreadyCompleted() {
            // given
            CompensationTxLog txLog = CompensationTxLog.create(1L, 1L, 1L, 1000);
            txLog.markAsCompleted();

            // when & then
            assertThatThrownBy(() -> compensationTxLogService.markAsFailed(txLog))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("getAllRetriables 메서드 테스트")
    class GetAllRetriablesTest {

        @Test
        @DisplayName("성공 : PENDING 상태의 보상 트랜잭션 로그 목록을 반환한다.")
        void shouldReturnPendingLogs() {
            // given
            CompensationTxLog txLog1 = CompensationTxLog.create(1L, 1L, 1L, 1000);
            CompensationTxLog txLog2 = CompensationTxLog.create(2L, 2L, 2L, 2000);

            when(compensationTxLogRepository.findAllByStatus(CompensationTxStatus.PENDING))
                    .thenReturn(List.of(txLog1, txLog2));

            // when
            List<CompensationTxLog> result = compensationTxLogService.getAllRetriables();

            // then
            verify(compensationTxLogRepository, times(1)).findAllByStatus(CompensationTxStatus.PENDING);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("성공 : PENDING 상태의 보상 트랜잭션 로그가 없으면 빈 목록을 반환한다.")
        void shouldReturnEmptyList_WhenNoPendingLogs() {
            // given
            when(compensationTxLogRepository.findAllByStatus(CompensationTxStatus.PENDING))
                    .thenReturn(Collections.emptyList());

            // when
            List<CompensationTxLog> result = compensationTxLogService.getAllRetriables();

            // then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getById 메서드 테스트")
    class GetByIdTest {

        @Test
        @DisplayName("실패 : 존재하지 않는 ID로 조회하면 UnavailableRequestException이 발생하며 실패한다.")
        void shouldThrowUnavailableRequestException_WhenNotFound() {
            // given
            Long txLogId = 999L;

            when(compensationTxLogRepository.findById(txLogId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> compensationTxLogService.getById(txLogId))
                    .isInstanceOf(UnavailableRequestException.class);
        }
    }
}
