package com.slam.concertreservation.domain.point.event;

public record PaymentEvent(
        Long userId,
        Integer point,
        Long reservationId) {
    public static PaymentEvent fromDomain(Long userId, Integer point, Long reservationId) {
        return new PaymentEvent(
                userId,
                point,
                reservationId);
    }
}
