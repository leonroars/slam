package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.point.model.Point;
import com.hhp7.concertreservation.domain.point.model.PointTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.hhp7.concertreservation.domain.point.model.PointHistory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class PointHistoryJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "point_history_id")
    private String id;
    private String userId;
    private int point;
    private String transactionType;
    private String description;


    public static PointHistory toDomainModel(PointHistoryJpaEntity entity) {
        return new PointHistory(
                entity.getId(),
                entity.getUserId(),
                PointTransactionType.valueOf(entity.getTransactionType()),
                entity.getPoint(),
                entity.getCreated_at()
        );
    }

    public static PointHistoryJpaEntity fromDomainModel(PointHistory pointHistory) {
        PointHistoryJpaEntity entity = new PointHistoryJpaEntity();
        entity.id = pointHistory.pointHistoryId();
        entity.userId = pointHistory.userId();
        entity.point = pointHistory.transactionAmount();
        entity.transactionType = String.valueOf(pointHistory.transactionType());
        return entity;
    }
}
