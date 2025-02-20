package com.hhp7.concertreservation.application.event.listener;

import com.hhp7.concertreservation.domain.concert.event.SeatUnassignedEvent;
import com.hhp7.concertreservation.domain.concert.service.ConcertService;
import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.service.ReservationRollbackService;
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
public class SeatUnassignedEventListener {

    private final ReservationRollbackService reservationRollbackService;
    private final ReservationService reservationService;
    private final ConcertService concertService;

    /**
     * 좌석 해제 이벤트 수신 시 공연 일정 상태 SOLDOUT -> UNAVAILABLE 변경
     * @param seatUnassignedEvent
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSeatUnassignedEvent(SeatUnassignedEvent seatUnassignedEvent) {
        concertService.makeConcertScheduleAvailable(seatUnassignedEvent.concertScheduleId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleSeatUnassignedEventRollback(SeatUnassignedEvent seatUnassignedEvent) {
        Reservation rollbackTargetReservation
                = reservationService.getReservationByConcertScheduleIdAndSeatId(seatUnassignedEvent.concertScheduleId(), seatUnassignedEvent.seatId());
        reservationRollbackService.rollbackExpireReservation(rollbackTargetReservation.getId());
    }


}
