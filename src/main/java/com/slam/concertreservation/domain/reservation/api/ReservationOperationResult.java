package com.slam.concertreservation.domain.reservation.api;

public record ReservationOperationResult(
        boolean success,
        Long reservationId,
        String errorCode
) {
    public static ReservationOperationResult success(Long reservationId) {
        return new ReservationOperationResult(true, reservationId, null);
    }

    public static ReservationOperationResult fail(Long reservationId, String errorCode) {
        return new ReservationOperationResult(false, reservationId, errorCode);
    }
}
