package com.hhp7.concertreservation.infrastructure.persistence.impl;

import com.hhp7.concertreservation.domain.user.model.User;
import com.hhp7.concertreservation.domain.user.repository.UserRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.UserJpaRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.UserJpaEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findUserByUserId(String userId) {
        return userJpaRepository.findByUserId(userId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public User joinUser(User user) {
        return userJpaRepository.save(UserJpaEntity.fromDomain(user)).toDomain();
    }
}
