package com.hhp7.concertreservation.application.event;

import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ReservationEventListener {

    private final RestTemplate mockDataPlatformApiClent;

        // WebClient 를 활용하여 API Call 수행 시 Netty 기반의 Non-Blocking I/O 가능하게 되므로
        // @Async 를 사용하지 않아도 됨. 하지만 Mocking 때문에 의존성 추가하고 싶지 않아 @Async + RestTemplate 사용
        @Async
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleReservationConfirmedEvent(ReservationConfirmationEvent event) {
            // 트랜잭션이 커밋된 후에 호출됨 - ok
            Reservation reservation = event.getConfirmedReservation();

            // 실제 데이터 플랫폼에 이벤트(혹은 API 호출) 전달
            mockDataPlatformApiClent.postForEntity("http://myconcert.com/data-platform-api/v1/reservation", reservation, Reservation.class);
        }
}