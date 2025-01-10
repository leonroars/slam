package com.hhp7.concertreservation.infrastructure.persistence.jpa;


import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.UserJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByUserId(String userId);
}
