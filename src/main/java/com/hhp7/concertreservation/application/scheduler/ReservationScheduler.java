package com.hhp7.concertreservation.application.scheduler;

import com.hhp7.concertreservation.domain.concert.service.ConcertService;
import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.service.ReservationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class ReservationScheduler {
    private final ReservationService reservationService;

    // 가예약 만료 처리
    @Scheduled(fixedDelay = 10000) // 약 10초 간격 순회하며 작업.
    public void expireTemporaryReservations() {
        List<Reservation> toBeExpired = reservationService.getReservationsToBeExpired();
        toBeExpired
                .forEach(reservation -> {
            reservationService.expireReservation(reservation.getId()); // 예약 상태 변경
        });
    }
}
