package com.slam.concertreservation.domain.dataplatform.application.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slam.concertreservation.domain.reservation.event.ReservationConfirmationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConfirmStorageListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reservation-confirm", groupId = "dataplatform-group")
    public void consumeReservationConfirm(String message) {
        try {
            ReservationConfirmationEvent event = objectMapper.readValue(message, ReservationConfirmationEvent.class);
            log.info("성공적으로 예약 확정 정보 수령! : [{}]", event.reservationId());
        } catch (Exception e) {
            log.error("Error processing message in dataplatform consumer: {}", e.getMessage());
        }
    }
}
