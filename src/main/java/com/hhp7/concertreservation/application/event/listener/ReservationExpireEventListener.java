package com.hhp7.concertreservation.application.event.listener;

import com.hhp7.concertreservation.domain.concert.service.ConcertService;
import com.hhp7.concertreservation.domain.reservation.event.ReservationExpirationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireEventListener {

    private final ConcertService concertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationExpireEvent(ReservationExpirationEvent reservationExpirationEvent) {

        // 가예약 만료 트랜잭션 정상 처리 시 해당 가예약이 선점하던 좌석 해제.
        concertService.unassignSeatOfConcertSchedule(
                reservationExpirationEvent.concertScheduleId()
                , reservationExpirationEvent.seatId()
        );

        log.warn("가예약 만료로 인한 좌석 선점 해제 완료: concertScheduleId: {}, seatId: {}",
                reservationExpirationEvent.concertScheduleId(),
                reservationExpirationEvent.seatId());
    }


}
