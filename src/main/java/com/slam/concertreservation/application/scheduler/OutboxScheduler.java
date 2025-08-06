package com.slam.concertreservation.application.scheduler;

import com.slam.concertreservation.infrastructure.outbox.OutboxJpaEntity;
import com.slam.concertreservation.infrastructure.outbox.OutboxService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    //
    @Scheduled(fixedDelay = 10)
    public void sendOutboxMessages() {
        log.info("아웃박스 메세지 전송 시작");
        List<OutboxJpaEntity> pending = outboxService.getAllPendingOutbox();
        log.info("아웃박스 메세지 전송 대상 수 : {}", pending.size());
        for (OutboxJpaEntity entity : pending) {
            try {
                // Topic 이름
                String topic = entity.getTopicIdentifier();
                kafkaTemplate.send(topic, entity.getPayload()).get();
                entity.updateToSent();
                outboxService.put(entity);
                log.info("아웃박스 저장내역 카프카로 발행 성공 : {}", entity.getId());
            } catch (Exception e) {
                log.error("Error sending outbox event id {}: {}", entity.getId(), e.getMessage());
                entity.increaseRetryCount();
                if (entity.isExceedMaxRetryCount()) {
                    entity.updateToError();
                }
                outboxService.put(entity);
            }
        }
    }

    // 아웃박스 전송 완료 항목 제거
    @Scheduled(fixedDelay = 10000) // 10초 간격 순회하며 작업.
    public void removeSentOutbox() {
        outboxService.deleteAllSent();
    }

    @Scheduled(fixedDelay = 3000) // 3초 간격 순회하며 작업.
    public void retryErrorOutbox() {
        outboxService.retryAllError();
    }


}
