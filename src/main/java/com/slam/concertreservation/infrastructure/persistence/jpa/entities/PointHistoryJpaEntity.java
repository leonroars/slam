package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.point.model.PointTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.slam.concertreservation.domain.point.model.PointHistory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(name = "POINTHISTORY")
public class PointHistoryJpaEntity {

    @Id
    @Column(name = "point_history_id")
    private String id;
    private String userId;
    private int point;
    private String transactionType;
    private String description;
    private LocalDateTime transactionDate;


    public static PointHistory toDomain(PointHistoryJpaEntity entity) {
        return new PointHistory(
                entity.getId(),
                entity.getUserId(),
                PointTransactionType.valueOf(entity.getTransactionType()),
                entity.getPoint(),
                entity.getTransactionDate()
        );
    }

    public static PointHistoryJpaEntity fromDomain(PointHistory pointHistory) {
        PointHistoryJpaEntity entity = new PointHistoryJpaEntity();
        entity.id = pointHistory.pointHistoryId();
        entity.userId = pointHistory.userId();
        entity.point = pointHistory.transactionAmount();
        entity.transactionType = String.valueOf(pointHistory.transactionType());
        return entity;
    }
}
