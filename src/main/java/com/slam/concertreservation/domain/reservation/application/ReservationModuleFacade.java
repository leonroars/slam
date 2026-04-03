package com.slam.concertreservation.domain.reservation.application;

import com.slam.concertreservation.domain.reservation.api.ReservationModuleApi;
import com.slam.concertreservation.domain.reservation.api.ReservationOperationResult;
import com.slam.concertreservation.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 논리적으로 분리된 다른 도메인에서의 예약 확정/취소 관련 기능을 제공하는 퍼사드 클래스.
 */

@Component
@RequiredArgsConstructor
public class ReservationModuleFacade implements ReservationModuleApi {

    private final ReservationService reservationService;

    @Override
    public ReservationOperationResult confirmReservation(Long reservationId) {
        try {
            reservationService.confirmReservation(reservationId);
            return ReservationOperationResult.success(reservationId);
        }
        catch (Exception e) {
            return ReservationOperationResult.fail(reservationId, e.getMessage());
        }
    }

    @Override
    public ReservationOperationResult cancelReservation(Long reservationId) {
        try {
            reservationService.cancelReservation(reservationId);
            return ReservationOperationResult.success(reservationId);
        }
        catch (Exception e) {
            return ReservationOperationResult.fail(reservationId, e.getMessage());
        }
    }
}
