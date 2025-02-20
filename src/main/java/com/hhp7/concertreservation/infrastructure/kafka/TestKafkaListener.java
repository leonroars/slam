package com.hhp7.concertreservation.infrastructure.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
@Getter
public class TestKafkaListener {

    private CountDownLatch latch = new CountDownLatch(1);
    private String receivedMessage;

    @KafkaListener(topics = "test-topic", groupId = "test-group")
    public void listen(String message) {
        log.info("Received Kafka message: {}", message);
        this.receivedMessage = message;
        latch.countDown();
    }

    // Helper to reset the latch if needed
    public void reset() {
        latch = new CountDownLatch(1);
    }
}