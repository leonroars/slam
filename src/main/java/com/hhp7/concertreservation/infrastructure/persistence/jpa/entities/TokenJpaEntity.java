package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.queue.model.Token;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Entity
@Table(name = "QUEUE")
@Getter
public class TokenJpaEntity extends BaseJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String concertScheduleId;
    private String status; // WAIT, ACTIVE, EXPIRED
    private LocalDateTime expiredAt;

    @Version
    private long version; // Row 단위 Optimistic Locking을 위한 Version 필드.

    public static TokenJpaEntity fromDomain(com.hhp7.concertreservation.domain.queue.model.Token domainModel) {
        TokenJpaEntity entity = new TokenJpaEntity();
        entity.userId = domainModel.getUserId();
        entity.concertScheduleId = domainModel.getConcertScheduleId();
        entity.status = domainModel.getStatus().name();
        entity.expiredAt = domainModel.getExpiredAt();

        return entity;
    }

    public Token toDomain() {
        return Token.create(
                this.userId,
                this.concertScheduleId,
                this.status,
                this.getCreated_at(),
                this.expiredAt);
    }

    public static List<TokenJpaEntity> createTokenJpaEntitiesFromDomain(List<Token> domainModels) {
        return domainModels.stream()
                .map(TokenJpaEntity::fromDomain)
                .toList();
    }
}
