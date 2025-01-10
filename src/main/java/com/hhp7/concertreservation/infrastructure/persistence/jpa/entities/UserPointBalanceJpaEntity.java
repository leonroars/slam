package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.point.model.Point;
import com.hhp7.concertreservation.domain.point.model.UserPointBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "POINTBALANCE")
public class UserPointBalanceJpaEntity extends BaseJpaEntity{

    @Id @GeneratedValue
    @Column(name = "point_balance_id")
    private Long id;
    private String userId;
    private int point;

    // UserPointModel(Domain Model) -> UserPointBalanceJpaEntity(JPA entity model)
    public static UserPointBalanceJpaEntity fromDomainModel(UserPointBalance domainModel) {
        UserPointBalanceJpaEntity entity = new UserPointBalanceJpaEntity();
        entity.userId = domainModel.userId();
        entity.point = domainModel.balance().getAmount();
        return entity;
    }


    // UserPointBalanceJpaEntity(JPA entity model) -> UserPointModel(Domain Model)
    public UserPointBalance toDomainModel() {
        return new UserPointBalance(this.userId, Point.create(this.point));
    }
}
