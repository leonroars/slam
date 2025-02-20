package com.hhp7.concertreservation.domain.point.event;

public record PaymentEvent(
        String userId,
        Integer point,
        String reservationId
) {
    public static PaymentEvent fromDomain(String userId, Integer point, String reservationId){
        return new PaymentEvent(
                userId,
                point,
                reservationId
        );
    }
}
