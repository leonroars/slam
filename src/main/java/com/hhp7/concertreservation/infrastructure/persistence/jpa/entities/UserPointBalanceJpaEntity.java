package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.point.model.Point;
import com.hhp7.concertreservation.domain.point.model.UserPointBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Getter
@Table(name = "POINTBALANCE")
public class UserPointBalanceJpaEntity extends BaseJpaEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_balance_id")
    private Long id;
    private String userId;
    private int point;

    @Version
    private Long version; // 낙관적 락 적용을 위한 Version 필드.

    // UserPointModel(Domain Model) -> UserPointBalanceJpaEntity(JPA entity model)
    public static UserPointBalanceJpaEntity fromDomain(UserPointBalance domainModel) {
        UserPointBalanceJpaEntity entity = new UserPointBalanceJpaEntity();
        if(domainModel.id() != null && !domainModel.id().isBlank()){
            entity.id = Long.valueOf(domainModel.id());
        }
        entity.userId = domainModel.userId();
        entity.point = domainModel.balance().getAmount();
        return entity;
    }


    // UserPointBalanceJpaEntity(JPA entity model) -> UserPointModel(Domain Model)
    public UserPointBalance toDomain() {
        return UserPointBalance.create(String.valueOf(this.id), this.userId, Point.create(this.point));
    }

    // UserPointBalanceJpaEntity 내에 updateFromDomain 구현 예시
    public UserPointBalanceJpaEntity updateFromDomain(UserPointBalance domain) {
        // this.id, this.version 은 그대로 유지
        this.userId = domain.userId();
        this.point = domain.balance().getAmount();
        return this;
    }
}
