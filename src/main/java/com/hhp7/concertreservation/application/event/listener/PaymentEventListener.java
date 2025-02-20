package com.hhp7.concertreservation.application.event.listener;

import com.hhp7.concertreservation.domain.point.event.PaymentEvent;
import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final ReservationService reservationService;


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentEvent(PaymentEvent paymentEvent){
        Reservation confirmedReservation = reservationService.confirmReservation(paymentEvent.reservationId());
        log.info("해당 예약 상태 로깅 : {}", confirmedReservation.getStatus());
        log.info("확정된 예약 ID : {}", confirmedReservation.getId());
        log.info("결제 완료 이벤트 수신: reservationId: {}", paymentEvent.reservationId());
    }
}
