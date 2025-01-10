package com.hhp7.concertreservation.infrastructure.persistence.jpa.impl;

import com.hhp7.concertreservation.domain.point.model.UserPointBalance;
import com.hhp7.concertreservation.domain.point.repository.UserPointBalanceRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.UserPointBalanceJpaRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.UserPointBalanceJpaEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPointBalanceRepositoryImpl implements UserPointBalanceRepository {

    private final UserPointBalanceJpaRepository userPointBalanceJpaRepository;

    @Override
    public Optional<UserPointBalance> getBalanceByUserId(String userId) {
        return userPointBalanceJpaRepository.findByUserId(userId)
                .map(UserPointBalanceJpaEntity::toDomainModel);
    }

    @Override
    public UserPointBalance save(UserPointBalance userPointBalance) {
        return userPointBalanceJpaRepository.save(
                        UserPointBalanceJpaEntity.fromDomainModel(userPointBalance))
                .toDomainModel();
    }
}
