package com.slam.concertreservation.domain.point.service;

import com.slam.concertreservation.domain.point.model.Point;
import com.slam.concertreservation.domain.point.model.PointHistory;
import com.slam.concertreservation.domain.point.model.PointTransactionType;
import com.slam.concertreservation.domain.point.model.UserPointBalance;
import com.slam.concertreservation.domain.point.repository.PointHistoryRepository;
import com.slam.concertreservation.domain.point.repository.UserPointBalanceRepository;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PointServiceUnitTest {

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserPointBalanceRepository userPointBalanceRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PointService pointService;

    private final String userId = "userId";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    class PointUsageUnitTest {

        @Test
        @DisplayName("성공 : 존재하는 사용자에 대하여 적법한 금액의 잔액 차감 시도 시 실제 잔액이 차감되고 차감 내역이 생성된다.")
        void shouldSuccessDecreasingUserPointBalance_WhenUserExistsAndProperAmountDecreased() {
            // given
            int currentBalance = 1000;
            int decreaseAmount = 300;

            UserPointBalance userPointBalance = UserPointBalance.create(userId, Point.create(currentBalance));
            UserPointBalance expected = UserPointBalance.create(userId, Point.create(currentBalance - decreaseAmount));

            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.of(userPointBalance));
            when(userPointBalanceRepository.save(any(UserPointBalance.class)))
                    .thenReturn(expected);

            // when
            UserPointBalance actual = pointService.decreaseUserPointBalance(userId, decreaseAmount);

            // then
            // 1) 레포지토리에서 정상적으로 조회되었는지 확인
            verify(userPointBalanceRepository, times(1)).getBalanceByUserId(userId);

            // 2) 포인트 히스토리가 저장되었는지 확인
            verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));

            // 3) 변경된 UserPointBalance가 저장되었는지 확인
            verify(userPointBalanceRepository, times(1)).save(userPointBalance);

            // 4) 잔액 검증
            assertEquals(expected.balance(), actual.balance());
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 사용자에 대한 포인트 차감 시도는 UnavailableRequestException 발생하며 실패한다.")
        void shouldThrowUnavailableRequestExceptionForDecrease_WhenUserDoesNotExist() {
            // given
            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.empty());

            // when & then
            Assertions.assertThatThrownBy(() -> pointService.decreaseUserPointBalance(userId, 1))
                    .isInstanceOf(UnavailableRequestException.class);
        }
    }

    @Nested
    class PointChargeUnitTest {

        @Test
        @DisplayName("성공 : 존재하는 사용자에 대하여 적법한 금액의 잔액 충전 시도 시 실제 잔액이 충전되고 충전 내역이 생성된다.")
        void shouldSuccessIncreasingUserPointBalance_WhenUserExistsAndProperAmountIncreased() {
            // given
            int currentBalance = 500;
            int increaseAmount = 200;

            UserPointBalance userPointBalance = UserPointBalance.create(userId, Point.create(currentBalance));
            UserPointBalance expected = UserPointBalance.create(userId, Point.create(currentBalance + increaseAmount));

            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.of(userPointBalance));
            when(userPointBalanceRepository.save(any(UserPointBalance.class)))
                    .thenReturn(expected);

            // when
            UserPointBalance actual = pointService.increaseUserPointBalance(userId, increaseAmount);

            // then
            verify(userPointBalanceRepository, times(1)).getBalanceByUserId(userId);
            verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
            verify(userPointBalanceRepository, times(1)).save(userPointBalance);

            assertEquals(expected.balance(), actual.balance());
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 사용자에 대한 포인트 충전 시도는 UnavailableRequestException 을 발생시키며 실패한다.")
        void shouldThrowUnavailableRequestExceptionForIncrease_WhenUserDoesNotExist() {
            // given
            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThrows(UnavailableRequestException.class,
                    () -> pointService.increaseUserPointBalance(userId, 200));
        }
    }

    @Nested
    class PointBalanceUnitTest {

        @Test
        @DisplayName("성공 : 신규 사용자가 가입할 경우 잔액 0인 해당 사용자의 UserPointBalance가 생성되어 저장된다.")
        void shouldCreateUserPointBalanceWithZero_WhenNewUserHasJoined() {
            // given
            UserPointBalance expectedBalance = UserPointBalance.create(userId, Point.create(0));

            when(userPointBalanceRepository.save(any(UserPointBalance.class)))
                    .thenReturn(expectedBalance);
            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.of(expectedBalance));
            // when
            UserPointBalance actualBalance = pointService.createUserPointBalance(userId);

            // then
            verify(userPointBalanceRepository, times(1))
                    .save(any(UserPointBalance.class));

            assertEquals(expectedBalance.balance(), actualBalance.balance());
            assertEquals(userId, actualBalance.userId());
        }

        @Test
        @DisplayName("성공 : 존재하는 사용자의 포인트 잔액 조회는 성공한다.")
        void shouldSuccessAndReturnUserPointBalance_WhenUserExists() {
            // given
            UserPointBalance expected = UserPointBalance.create(userId, Point.create(1000));

            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.of(expected));
            when(userPointBalanceRepository.save(any(UserPointBalance.class)))
                    .thenReturn(expected);

            // when
            UserPointBalance actual = pointService.getUserPointBalance(userId);

            // then
            verify(userPointBalanceRepository, times(1)).getBalanceByUserId(userId);
            assertEquals(expected.balance(), actual.balance());
            assertEquals(userId, actual.userId());
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 사용자에 대한 포인트 잔액 조회 시도 시 UnavailableRequestException 발생하며 실패한다.")
        void shouldThrowUnavailableRequestException_WhenUserDoesNotExist() {
            // given
            String userId = "unknown";
            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThrows(UnavailableRequestException.class,
                    () -> pointService.getUserPointBalance(userId));
        }
    }


    @Nested
    class PointHistoryUnitTest {

        @Test
        @DisplayName("성공 : 존재하는 사용자에 대한 포인트 충전 시도 시 이와 내용이 일치하는 정확한 PointHistory 가 생성되어 저장된다.")
        void shouldCreateAndSavePointHistory_WhenTheLegalChargeAttemptIsProcessed(){
            // given
            int currentBalance = 500;
            int increaseAmount = 200;

            Point point = Point.create(currentBalance);
            UserPointBalance userPointBalance = UserPointBalance.create(userId, point);
            UserPointBalance expectedBalance = UserPointBalance.create(userId, Point.create(currentBalance + increaseAmount));
            PointHistory expectedPointHistory = PointHistory.create(userId, PointTransactionType.CHARGE, increaseAmount);

            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.of(userPointBalance));
            when(userPointBalanceRepository.save(any(UserPointBalance.class)))
                    .thenReturn(expectedBalance);
            when(pointHistoryRepository.save(any(PointHistory.class)))
                    .thenReturn(expectedPointHistory);
            when(pointHistoryRepository.findByUserId(userId))
                    .thenReturn(List.of(expectedPointHistory));

            // when
            pointService.increaseUserPointBalance(userId, increaseAmount);
            List<PointHistory> actualPointHistory = pointService.getUserPointHistories(userId);

            // then
            assertEquals(actualPointHistory.size(), 1);
            assertEquals(expectedPointHistory, actualPointHistory.get(0));
        }

        @Test
        @DisplayName("성공 : 존재하는 사용자에 대한 포인트 차감 시도 시 이와 내용이 일치하는 정확한 PointHistory 가 생성되어 저장된다.")
        void shouldCreateAndSavePointHistory_WhenTheLegalUsageAttemptIsProcessed() {
            // given
            int currentBalance = 500;
            int decreaseAmount = 200;

            Point point = Point.create(currentBalance);
            UserPointBalance userPointBalance = UserPointBalance.create(userId, point);
            UserPointBalance expectedBalance = UserPointBalance.create(userId, Point.create(currentBalance - decreaseAmount));
            PointHistory expectedPointHistory = PointHistory.create(userId, PointTransactionType.CHARGE, decreaseAmount);

            when(userPointBalanceRepository.getBalanceByUserId(userId))
                    .thenReturn(Optional.of(userPointBalance));
            when(userPointBalanceRepository.save(any(UserPointBalance.class)))
                    .thenReturn(expectedBalance);
            when(pointHistoryRepository.save(any(PointHistory.class)))
                    .thenReturn(expectedPointHistory);
            when(pointHistoryRepository.findByUserId(userId))
                    .thenReturn(List.of(expectedPointHistory));

            // when
            pointService.increaseUserPointBalance(userId, decreaseAmount);
            List<PointHistory> actualPointHistory = pointService.getUserPointHistories(userId);

            // then
            assertEquals(actualPointHistory.size(), 1);
            assertEquals(expectedPointHistory, actualPointHistory.get(0));
        }

    }
}