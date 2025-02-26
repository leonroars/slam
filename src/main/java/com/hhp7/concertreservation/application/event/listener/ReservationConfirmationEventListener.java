package com.hhp7.concertreservation.application.event.listener;

import com.hhp7.concertreservation.application.event.publisher.OutboxDomainEventPublisher;
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
    private final OutboxDomainEventPublisher outboxPublisher;


    /**
     * 예약 확정 이벤트 롤백 시 -> 사용자 포인트 증가
     * @param reservationConfirmationEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleReservationConfirmationEventRollback(ReservationConfirmationEvent reservationConfirmationEvent) {
        log.info("예약 확정 이벤트 롤백: userId: {}, point: {}", reservationConfirmationEvent.userId(), reservationConfirmationEvent.price());
        pointService.increaseUserPointBalance(reservationConfirmationEvent.userId(), reservationConfirmationEvent.price());
    }

    /**
     * 예약 확정 이벤트 발생 시 -> 아웃박스 발행
     * <br></br>
     * 이벤트 발행 시점은 트랜잭션 커밋 전이며, 이벤트 발행 실패 시 롤백됩니다.
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReservationConfirmEvent(ReservationConfirmationEvent event) {
        // "ReservationService" is used as a 'from' identifier
        outboxPublisher.publish(event);
        log.info("해당 예약 확정 정보 아웃박스 저장 호출 : {}", event.reservationId());
    }


}
