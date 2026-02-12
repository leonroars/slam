package com.slam.concertreservation.domain.reservation.application;

import com.slam.concertreservation.domain.reservation.api.ReservationOperationResult;
import com.slam.concertreservation.domain.reservation.model.Reservation;
import com.slam.concertreservation.domain.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationModuleFacadeUnitTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationModuleFacade reservationModuleFacade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("confirmReservation 메서드 테스트")
    class ConfirmReservationTest {

        @Test
        @DisplayName("성공 : 예약 확정이 정상적으로 처리되면 success 결과를 반환한다.")
        void shouldReturnSuccess_WhenConfirmationSucceeds() {
            // given
            Long reservationId = 1L;
            Reservation reservation = Reservation.create(reservationId, 1L, 1L, 1L, 1000);
            reservation.confirm();

            when(reservationService.confirmReservation(reservationId)).thenReturn(reservation);

            // when
            ReservationOperationResult result = reservationModuleFacade.confirmReservation(reservationId);

            // then
            assertTrue(result.success());
            assertEquals(reservationId, result.reservationId());
            assertNull(result.errorCode());
            verify(reservationService, times(1)).confirmReservation(reservationId);
        }

        @Test
        @DisplayName("실패 : 예약 확정 중 예외가 발생하면 fail 결과를 반환한다.")
        void shouldReturnFail_WhenConfirmationThrowsException() {
            // given
            Long reservationId = 1L;
            String errorMessage = "예약 상태가 유효하지 않습니다.";

            when(reservationService.confirmReservation(reservationId))
                    .thenThrow(new RuntimeException(errorMessage));

            // when
            ReservationOperationResult result = reservationModuleFacade.confirmReservation(reservationId);

            // then
            assertFalse(result.success());
            assertEquals(reservationId, result.reservationId());
            assertEquals(errorMessage, result.errorCode());
            verify(reservationService, times(1)).confirmReservation(reservationId);
        }
    }

    @Nested
    @DisplayName("cancelReservation 메서드 테스트")
    class CancelReservationTest {

        @Test
        @DisplayName("성공 : 예약 취소가 정상적으로 처리되면 success 결과를 반환한다.")
        void shouldReturnSuccess_WhenCancellationSucceeds() {
            // given
            Long reservationId = 1L;
            Reservation reservation = Reservation.create(reservationId, 1L, 1L, 1L, 1000);
            reservation.confirm();
            reservation.cancel();

            when(reservationService.cancelReservation(reservationId)).thenReturn(reservation);

            // when
            ReservationOperationResult result = reservationModuleFacade.cancelReservation(reservationId);

            // then
            assertTrue(result.success());
            assertEquals(reservationId, result.reservationId());
            assertNull(result.errorCode());
            verify(reservationService, times(1)).cancelReservation(reservationId);
        }

        @Test
        @DisplayName("실패 : 예약 취소 중 예외가 발생하면 fail 결과를 반환한다.")
        void shouldReturnFail_WhenCancellationThrowsException() {
            // given
            Long reservationId = 1L;
            String errorMessage = "예약을 취소할 수 없습니다.";

            when(reservationService.cancelReservation(reservationId))
                    .thenThrow(new RuntimeException(errorMessage));

            // when
            ReservationOperationResult result = reservationModuleFacade.cancelReservation(reservationId);

            // then
            assertFalse(result.success());
            assertEquals(reservationId, result.reservationId());
            assertEquals(errorMessage, result.errorCode());
            verify(reservationService, times(1)).cancelReservation(reservationId);
        }
    }
}
