package com.hhp7.concertreservation.infrastructure.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxJpaEntity, String> {

    @Query("select o from OutboxJpaEntity o where o.status = com.hhp7.concertreservation.infrastructure.outbox.OutboxStatus.PENDING")
    List<OutboxJpaEntity> findPendingOutbox();

    @Query("select o from OutboxJpaEntity o where o.status = com.hhp7.concertreservation.infrastructure.outbox.OutboxStatus.ERROR")
    List<OutboxJpaEntity> findErrorOutbox();

    @Query("select o from OutboxJpaEntity o where o.status = com.hhp7.concertreservation.infrastructure.outbox.OutboxStatus.ERROR and o.retryCount >= 5")
    List<OutboxJpaEntity> findExceedMaxRetryCountOutbox();

    @Query("select o from OutboxJpaEntity o where o.status = com.hhp7.concertreservation.infrastructure.outbox.OutboxStatus.SENT")
    List<OutboxJpaEntity> findAllSent();

}
