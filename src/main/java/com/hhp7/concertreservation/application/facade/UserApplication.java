package com.hhp7.concertreservation.application.facade;

import com.hhp7.concertreservation.domain.point.service.PointService;
import com.hhp7.concertreservation.domain.user.model.User;
import com.hhp7.concertreservation.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserApplication {
    private final UserService userService;
    private final PointService pointService;

    /**
     * 회원 가입.
     * @return
     */
    public User registerUser(String name) {
        User savedUser = userService.joinUser(User.create(name)); // 회원 생성 및 저장
        pointService.createUserPointBalance(savedUser.getId()); // 신규 회원 잔액 0 생성.
        return savedUser;
    }

    /**
     * 회원 조회.
     * @param userId
     * @return
     */
    public User getUser(String userId) {
        return userService.findUserByUserId(userId);
    }
}
