package com.slam.concertreservation.application.event.listener;

import com.slam.concertreservation.domain.concert.service.ConcertService;
import com.slam.concertreservation.domain.reservation.event.ReservationCreationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCreatedEventListener {

    private final ConcertService concertService;

    /**
     * 예약 생성 로직 처리 중 롤백 발생 시 -> 좌석 선점 해제
     * 
     * @param reservationCreationEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleReservationCreatedEvent(ReservationCreationEvent reservationCreationEvent) {

        concertService.unassignSeatOfConcertSchedule(
                reservationCreationEvent.concertScheduleId(),
                reservationCreationEvent.seatId());
        log.warn("예약 생성 롤백으로 인한 좌석 선점 해제 완료: concertScheduleId: {}, seatId: {}",
                reservationCreationEvent.concertScheduleId(),
                reservationCreationEvent.seatId());
    }

}
