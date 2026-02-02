package com.slam.concertreservation.application.facade;

import com.slam.concertreservation.domain.point.service.PointService;
import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserApplication {
    private final UserService userService;
    private final PointService pointService;

    /**
     * 회원 가입.
     * 
     * @return
     */
    @Transactional
    public User registerUser(String name) {
        User savedUser = userService.joinUser(User.create(name)); // 회원 생성 및 저장
        pointService.createUserPointBalance(savedUser.getId()); // 신규 회원 잔액 0 생성.
        return savedUser;
    }

    /**
     * 회원 조회.
     * 
     * @param userId
     * @return
     */
    public User getUser(Long userId) {
        return userService.findUserByUserId(userId);
    }
}
