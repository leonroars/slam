package com.hhp7.concertreservation.application.event.listener;

import com.hhp7.concertreservation.domain.point.service.PointService;
import com.hhp7.concertreservation.domain.reservation.event.ReservationConfirmationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConfirmationEventListener {

    private final PointService pointService;

    /**
     * 예약 확정 이벤트 수신 시 -> 사용자 포인트 차감
     * @param reservationConfirmationEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationConfirmationEvent(ReservationConfirmationEvent reservationConfirmationEvent) {
        // 로깅
        log.info("예약 확정 이벤트 수신: userId: {}, point: {}", reservationConfirmationEvent.userId(), reservationConfirmationEvent.price());
    }

    /**
     * 예약 확정 이벤트 롤백 시 -> 사용자 포인트 증가
     * @param reservationConfirmationEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleReservationConfirmationEventRollback(ReservationConfirmationEvent reservationConfirmationEvent) {
        pointService.increaseUserPointBalance(reservationConfirmationEvent.userId(), reservationConfirmationEvent.price());
    }


}
