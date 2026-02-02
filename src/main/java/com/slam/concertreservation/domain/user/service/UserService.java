package com.slam.concertreservation.domain.user.service;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.domain.user.repository.UserRepository;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    // 회원 가입
    public User joinUser(User user) {
        return userRepository.saveUser(user);
    }

    // 회원 조회
    public User findUserByUserId(Long userId) {
        return userRepository.findUserByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.USER_NOT_FOUND,
                        "해당 사용자가 존재하지 않으므로 조회가 불가합니다."));
    }
}
