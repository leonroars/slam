package com.slam.concertreservation.domain.reservation.api;

import com.slam.concertreservation.interfaces.dto.ReservationCancelResponse;
import com.slam.concertreservation.interfaces.dto.ReservationConfirmResponse;

/**
 * 예약 API 인터페이스
 */
public interface ReservationModuleApi {

    ReservationOperationResult confirmReservation(Long reservationId);

    ReservationOperationResult cancelReservation(Long reservationId);
}
