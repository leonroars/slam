package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.point.model.PointHistory;
import com.slam.concertreservation.domain.point.repository.PointHistoryRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.PointHistoryJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.PointHistoryJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {
    private final PointHistoryJpaRepository pointHistoryJpaRepository;

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return PointHistoryJpaEntity.toDomain(
                pointHistoryJpaRepository
                        .save(PointHistoryJpaEntity.fromDomain(pointHistory))
        );
    }

    @Override
    public List<PointHistory> findByUserId(String userId) {
        return pointHistoryJpaRepository
                .findByUserId(userId)
                .stream()
                .map(PointHistoryJpaEntity::toDomain)
                .toList();
    }
}
