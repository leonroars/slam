package com.hhp7.concertreservation.domain.user.service;

import com.hhp7.concertreservation.domain.user.model.User;
import com.hhp7.concertreservation.domain.user.repository.UserRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
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
    public User findUserByUserId(String userId) {
        return userRepository.findUserByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException("해당 사용자가 존재하지 않으므로 조회가 불가합니다."));
    }
}
