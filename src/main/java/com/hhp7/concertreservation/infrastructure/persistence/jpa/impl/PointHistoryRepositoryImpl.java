package com.hhp7.concertreservation.infrastructure.persistence.jpa.impl;

import com.hhp7.concertreservation.domain.point.model.PointHistory;
import com.hhp7.concertreservation.domain.point.repository.PointHistoryRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.PointHistoryJpaRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.PointHistoryJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {
    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return PointHistoryJpaEntity.toDomainModel(
                pointHistoryJpaRepository
                        .save(PointHistoryJpaEntity.fromDomainModel(pointHistory))
        );
    }

    @Override
    public List<PointHistory> findByUserId(String userId) {
        return pointHistoryJpaRepository
                .findByUserId(userId)
                .stream()
                .map(PointHistoryJpaEntity::toDomainModel)
                .toList();
    }
}
