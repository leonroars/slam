package com.slam.concertreservation.infrastructure.persistence.impl;

import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.domain.user.repository.UserRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.UserJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.UserJpaEntity;
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
    public User saveUser(User user) {
        return userJpaRepository.save(UserJpaEntity.fromDomain(user)).toDomain();
    }
}
