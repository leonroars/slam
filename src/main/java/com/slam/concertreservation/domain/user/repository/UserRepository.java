package com.slam.concertreservation.domain.user.repository;

import com.slam.concertreservation.domain.user.model.User;
import java.util.Optional;

public interface UserRepository {

    // 회원 조회
    Optional<User> findUserByUserId(Long userId);

    // 회원 가입
    User saveUser(User user);
}
