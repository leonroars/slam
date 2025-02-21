package com.hhp7.concertreservation.domain.reservation.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhp7.concertreservation.domain.reservation.event.ReservationConfirmationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxSelfConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reservation-confirmation", groupId = "publisher-self-group")
    public void consumeSelf(String message) {
        try {
            ReservationConfirmationEvent event = objectMapper.readValue(message, ReservationConfirmationEvent.class);
            log.info("예약 확정 이벤트를 담은 메세지 발행 확인: [{}]", event.reservationId());

        } catch (Exception e) {
            log.error("셀프 컨슈머에서 문제 발생: {}", e.getMessage());
        }
    }
}