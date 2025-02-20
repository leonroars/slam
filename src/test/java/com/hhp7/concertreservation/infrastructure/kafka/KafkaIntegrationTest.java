package com.hhp7.concertreservation.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TestKafkaListener testKafkaListener;

    @Test
    @DisplayName("카프카 메시지 전송 및 수신 테스트")
    public void shouldReceiveMessageFromKafka() throws InterruptedException {
        // Reset listener latch
        testKafkaListener.reset();

        // Send a test message to "test-topic"
        String testMessage = "카프카 제발 되주세요 제발";
        kafkaTemplate.send("test-topic", testMessage);

        // Wait up to 10 seconds for the message to be received
        boolean messageReceived = testKafkaListener.getLatch().await(1, TimeUnit.SECONDS);
        assertTrue(messageReceived, "The message was not received within the timeout period");

        // Assert that the received message matches what was sent
        assertEquals(testMessage, testKafkaListener.getReceivedMessage());
    }
}