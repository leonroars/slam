package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.domain.point.model.UserPointBalance;
import com.slam.concertreservation.domain.point.repository.UserPointBalanceRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.UserPointBalanceJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.UserPointBalanceJpaEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPointBalanceRepositoryImpl implements UserPointBalanceRepository {

    private final UserPointBalanceJpaRepository userPointBalanceJpaRepository;

    @Override
    public Optional<UserPointBalance> getBalanceByUserId(Long userId) {
        return userPointBalanceJpaRepository.findByUserId(userId)
                .map(UserPointBalanceJpaEntity::toDomain);
    }

    @Override
    public UserPointBalance save(UserPointBalance userPointBalance) {
        // 1) domain.id()가 존재하면, DB에서 엔티티를 조회 후 update
        if (userPointBalance.id() != null) {
            UserPointBalanceJpaEntity existingEntity = userPointBalanceJpaRepository
                    .findById(userPointBalance.id())
                    .orElseThrow(() -> new BusinessRuleViolationException(ErrorCode.INTERNAL_SERVER_ERROR,
                            "존재하지 않는 PointBalance ID입니다."));

            // 기존 엔티티의 version, id 유지, point 등만 갱신
            existingEntity = existingEntity.updateFromDomain(userPointBalance);

            UserPointBalanceJpaEntity saved = userPointBalanceJpaRepository.save(existingEntity);
            return saved.toDomain();

        } else {
            // 2) domain.id()가 없으면 신규 생성
            UserPointBalanceJpaEntity newEntity = UserPointBalanceJpaEntity.fromDomain(userPointBalance);
            UserPointBalanceJpaEntity saved = userPointBalanceJpaRepository.save(newEntity);
            return saved.toDomain();
        }
    }
}
