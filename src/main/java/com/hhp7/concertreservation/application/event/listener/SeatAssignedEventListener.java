package com.hhp7.concertreservation.application.event.listener;

import com.hhp7.concertreservation.domain.concert.event.SeatAssignedEvent;
import com.hhp7.concertreservation.domain.concert.service.ConcertService;
import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좌석 할당 이벤트 리스너
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatAssignedEventListener {

    private final ReservationService reservationService;
    private final ConcertService concertService;


    /**
     * 좌석 할당 이벤트 발생 시 -> 가예약 생성
     * @param seatAssignedEvent
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 좌석 선점 롤백 시 이벤트 발행되지 않는다.
    public void handleSeatAssignedEventWithReservationCreation(SeatAssignedEvent seatAssignedEvent) {
        Reservation createdReservation = reservationService.createReservation(
                seatAssignedEvent.userId(),
                seatAssignedEvent.concertScheduleId(),
                seatAssignedEvent.seatId(),
                seatAssignedEvent.price()
        ); // 가예약 생성
    }

    /**
     * 좌석 할당 이벤트 발생 시 -> 공연 일정 상태 변경
     * @param seatAssignedEvent
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSeatAssignedEventWithChangingConcertScheduleStatus(SeatAssignedEvent seatAssignedEvent) {
        concertService.makeConcertScheduleSoldOut(seatAssignedEvent.concertScheduleId());
    }

    /**
     * 좌석 할당 이벤트 롤백 시 -> 공연 일정 상태를 다시 AVAILABLE로 변경
     * <br></br>
     * 공연 일정 상태 변경이 일어난 적 없거나 롤백이 필요하지 않은 상황은 makeConcertScheduleAvailable 메서드 내부에서 처리
     * @param seatAssignedEvent
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSeatAssignedEventRollbackWithChangingConcertScheduleStatus(SeatAssignedEvent seatAssignedEvent) {
        concertService.makeConcertScheduleAvailable(seatAssignedEvent.concertScheduleId());
    }
}
