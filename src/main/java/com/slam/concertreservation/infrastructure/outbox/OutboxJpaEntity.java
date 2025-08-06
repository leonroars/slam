package com.slam.concertreservation.infrastructure.outbox;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxJpaEntity extends BaseJpaEntity {

    @Id
    private String id;

    @Column(columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    private OutboxStatus status;

    private String topicIdentifier; // 토픽 식별자

    private int retryCount;

    public void increaseRetryCount() {
        retryCount++;
    }

    public boolean isExceedMaxRetryCount() {
        return retryCount > 5;
    }

    public OutboxJpaEntity updateToSent() {
        this.status = OutboxStatus.SENT;
        return this;
    }

    public OutboxJpaEntity updateToError() {
        this.status = OutboxStatus.ERROR;
        this.retryCount = 0;
        return this;
    }

    public OutboxJpaEntity initiateStatus() {
        this.status = OutboxStatus.PENDING;
        return this;
    }


}
