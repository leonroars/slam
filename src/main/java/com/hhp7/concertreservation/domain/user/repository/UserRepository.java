package com.hhp7.concertreservation.domain.user.repository;

import com.hhp7.concertreservation.domain.user.model.User;
import java.util.Optional;

public interface UserRepository {

    // 회원 조회
    Optional<User> findUserByUserId(String userId);

    // 회원 가입
    User joinUser(User user);

}
