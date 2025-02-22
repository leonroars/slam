package com.hhp7.concertreservation.application.event.publisher;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhp7.concertreservation.infrastructure.outbox.OutboxJpaEntity;
import com.hhp7.concertreservation.infrastructure.outbox.OutboxRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도메인 이벤트를 아웃박스에 저장하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxDomainEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;  // Inject Jackson's ObjectMapper

    @Transactional
    public void publish(Object domainEvent, String from) {
        try {
            // Serialize the domain event to JSON
            String payload = objectMapper.writeValueAsString(domainEvent);

            // Create an outbox entity with the payload and metadata
            OutboxJpaEntity outboxEntity = OutboxJpaEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .payload(payload)
                    .topicIdentifier(determineTopicName(domainEvent.getClass()))
                    .eventType(domainEvent.getClass().getName())
                    .retryCount(0)
                    .build()
                    .initiateStatus();

            // Save the outbox entity in the same transaction as your domain change

            outboxRepository.save(outboxEntity);
            log.info("아웃박스로 저장 성공 : {} / 아웃박스 토픽 이름 : {}", outboxEntity.getId(), outboxEntity.getTopicIdentifier());

        } catch (JsonProcessingException e) {
            log.info("아웃박스로 저장 실패 : {}", e.getMessage());
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    /**
     * 도메인 이벤트 클래스 이름을 topic 이름으로 변환합니다.
     * @param eventClassName
     * @return
     */
    private String determineTopicName(Class<?> eventClass) {
        String simpleName = eventClass.getSimpleName(); // e.g. "ReservationConfirmationEvent"
        // Remove "Event" suffix
        if(simpleName.endsWith("Event")){
            simpleName = simpleName.substring(0, simpleName.length() - "Event".length());
        }
        // Convert camelCase to hyphen-delimited lower-case
        return simpleName.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}