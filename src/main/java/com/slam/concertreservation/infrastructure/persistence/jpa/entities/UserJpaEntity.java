package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "`USER`")
@Getter
@NoArgsConstructor
public class UserJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId; // Domain Model 의 ID(TSID Long)를 함께 사용합니다.
    private String name;

    public UserJpaEntity(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    // User -> UserJpaEntity
    public static UserJpaEntity fromDomain(User domainUser) {
        return new UserJpaEntity(
                domainUser.getId(), domainUser.getName());
    }

    // UserJpaEntity -> User
    public User toDomain() {
        return User.create(
                this.getUserId(),
                this.getName());
    }

}
