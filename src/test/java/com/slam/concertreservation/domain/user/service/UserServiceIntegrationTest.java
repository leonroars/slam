package com.slam.concertreservation.domain.user.service;

import com.slam.concertreservation.domain.user.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;


    @Test
    @DisplayName("성공 : 회원 가입 시 회원 조회가 가능해진다.")
    void savedUserMustBeFound_AfterJoinUser() {
        // given
        User user = User.create("김선빈");

        // when
        User expectedUser = userService.joinUser(user);
        User actualUser = userService.findUserByUserId(expectedUser.getId());

        // then
        Assertions.assertNotNull(actualUser);
        Assertions.assertEquals(expectedUser, actualUser);
    }
}
