package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.queue.model.Token;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "'QUEUE'")
@Getter
public class TokenJpaEntity extends BaseJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String concertScheduleId;
    private String userId;
    private String status; // WAIT, ACTIVE, EXPIRED
    private LocalDateTime expiredAt;

    public static TokenJpaEntity fromDomain(Token domainModel) {
        TokenJpaEntity entity = new TokenJpaEntity();
        entity.id = (domainModel.getId() == null) ? null : Long.parseLong(domainModel.getId());
        entity.userId = domainModel.getUserId();
        entity.concertScheduleId = domainModel.getConcertScheduleId();
        entity.status = domainModel.getStatus().name();
        entity.expiredAt = domainModel.getExpiredAt();

        return entity;
    }

    public Token toDomain() {
        return Token.create(
                Long.toString(this.id),
                this.userId,
                this.concertScheduleId,
                this.status,
                this.getCreated_at(),
                this.expiredAt);
    }

    /**
     * Domain Model 의 변경사항을 Entity 에 반영.
     * <br></br>
     *  - 수정이라는 상황의 특성 상, '이미 한 번 저장되었음'을 의미. 따라서, ID필드에 대한 Update 는 제외하는 것이 안전하고 타당함.
     * @param domainModel
     * @return
     */
    public TokenJpaEntity updateFromDomain(Token domainModel) {
        this.userId = domainModel.getUserId();
        this.concertScheduleId = domainModel.getConcertScheduleId();
        this.status = domainModel.getStatus().name();
        this.expiredAt = domainModel.getExpiredAt();

        return this;
    }

    public static List<TokenJpaEntity> createTokenJpaEntitiesFromDomain(List<Token> domainModels) {
        return domainModels.stream()
                .map(TokenJpaEntity::fromDomain)
                .toList();
    }
}
