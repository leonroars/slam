package com.slam.concertreservation.infrastructure.persistence.jpa;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.UserPointBalanceJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPointBalanceJpaRepository extends JpaRepository<UserPointBalanceJpaEntity, String> {

    Optional<UserPointBalanceJpaEntity> findByUserId(String userId);
}
