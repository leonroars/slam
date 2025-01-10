package com.hhp7.concertreservation.domain.user.service;

import com.hhp7.concertreservation.domain.user.model.User;
import com.hhp7.concertreservation.domain.user.repository.UserRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.UserJpaRepository;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

// UserService 단위 테스트
public class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("실패 : 존재하지 않는 회원 조회 시 UnavailableRequestException 발생하며 실패한다.")
    void shouldThrowUnavailableRequestException_WhenUserNotFound(){
        // given
        String userId = "1";

        // when & then
        Mockito.when(userRepository.findUserByUserId(userId))
                .thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> {
                    userService.findUserByUserId(userId);
                })
                .isInstanceOf(UnavailableRequestException.class)
                .hasMessageContaining("해당 사용자가 존재하지 않으므로 조회가 불가합니다.");
    }

    @Test
    @DisplayName("성공 : 회원 가입 시 새로운 회원 조회가 가능해진다.")
    void shouldReturnUser_WhenNewUserIsSaved(){
        // given
        String userId = "1";
        String userName = "우도균";
        User expectedUser = User.create("우도균");

        Mockito.when(userRepository.findUserByUserId(userId))
                .thenReturn(Optional.of(expectedUser));
        // when
        User actualUser = userService.findUserByUserId(userId);

        // then
        Assertions.assertThat(actualUser).isEqualTo(expectedUser);
    }

}
