package com.hhp7.concertreservation.infrastructure.outbox;

import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public OutboxJpaEntity put(OutboxJpaEntity outboxJpaEntity) {
        return outboxRepository.save(outboxJpaEntity);
    }

    public OutboxJpaEntity get(String outboxId) {
        return outboxRepository.findById(outboxId)
                .orElseThrow(() -> new UnavailableRequestException("해당 아웃박스가 존재하지 않습니다."));
    }

    @Transactional
    public void remove(String outboxId) {
        outboxRepository.deleteById(outboxId);
    }

    @Transactional
    public OutboxJpaEntity markAsSent(String outboxId) {
        OutboxJpaEntity outboxJpaEntity = get(outboxId);
        outboxJpaEntity.updateToSent();
        return outboxRepository.save(outboxJpaEntity);
    }

    @Transactional
    public OutboxJpaEntity markAsError(String outboxId) {
        OutboxJpaEntity outboxJpaEntity = get(outboxId);
        outboxJpaEntity.updateToError();
        outboxJpaEntity.increaseRetryCount(); // 재시도횟수 증가.
        return outboxRepository.save(outboxJpaEntity);
    }

    @Transactional
    public void retry(String outboxId) {
        OutboxJpaEntity outboxJpaEntity = get(outboxId);
        outboxJpaEntity.increaseRetryCount();
        outboxRepository.save(outboxJpaEntity);
    }

    @Transactional
    public void retryAllError() {
        outboxRepository.findErrorOutbox().forEach(outboxJpaEntity -> {
            outboxJpaEntity.increaseRetryCount();
            outboxRepository.save(outboxJpaEntity);
        });
    }

    @Transactional
    public void deleteAllSent(){
        outboxRepository.deleteAll(outboxRepository.findAllSent());
    }

    public List<OutboxJpaEntity> getAllPendingOutbox() {
        return outboxRepository.findPendingOutbox();
    }

    public List<OutboxJpaEntity> getAllErrorOutbox() {
        return outboxRepository.findErrorOutbox();
    }

    public List<OutboxJpaEntity> getAllExceedMaxRetryCountOutbox() {
        return outboxRepository.findExceedMaxRetryCountOutbox();
    }


}
