package com.hhp7.concertreservation.application.event.publisher;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    public void publish(Object domainEvent) {
        try {
            // 도메인 이벤트를 직렬화
            String payload = objectMapper
                    .registerModule(new JavaTimeModule()) // LocalDateTime 직렬화를 위한 모듈 등록
                    .writeValueAsString(domainEvent);

            // 아웃박스 엔티티 생성
            OutboxJpaEntity outboxEntity = OutboxJpaEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .payload(payload)
                    .topicIdentifier(determineTopicName(domainEvent.getClass()))
                    .retryCount(0)
                    .build()
                    .initiateStatus();

            // 아웃박스 저장
            outboxRepository.save(outboxEntity);

            log.info("아웃박스로 저장 성공 : {} / 아웃박스 토픽 이름 : {}", outboxEntity.getId(), outboxEntity.getTopicIdentifier());

        } catch (Exception e) {
            log.info("아웃박스로 저장 실패 : {}", e.getMessage());
            throw new RuntimeException("예외가 발생하여 아웃박스 저장에 실패하였습니다.", e);
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