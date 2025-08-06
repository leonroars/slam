package com.slam.concertreservation.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.domain.user.repository.UserRepository;
import com.slam.concertreservation.exceptions.UnavailableRequestException;
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
    @DisplayName("성공 : 회원 가입 시 회원이 성공적으로 생성되어 저장된다.")
    void shouldCreateAndSaveUser_WhenNewUserJoins(){
        // given
        String userId = "1";
        String userName = "우도균";
        User expectedUser = User.create("우도균");

        Mockito.when(userRepository.saveUser(any(User.class))).thenReturn(expectedUser);
        // when
        User actualUser = userService.joinUser(expectedUser);

        // then
        assertEquals(expectedUser.getName(), actualUser.getName());
        verify(userRepository, times(1)).saveUser(any(User.class));
    }

    @Test
    @DisplayName("성공 : 이미 가입한 회원의 ID로 회원 조회 시 해당 회원이 성공적으로 조회된다.")
    void shouldReturnExistingUser_WhenThatUserIdExist(){
        // given
        String userId = "1";
        String userName = "김선빈";
        User expectedUser = User.create(userId, userName);

        Mockito.when(userRepository.findUserByUserId(userId)).thenReturn(Optional.of(expectedUser));

        // when
        User actualUser = userService.findUserByUserId(userId);

        // then
        assertEquals(expectedUser.getId(), actualUser.getId());
        assertEquals(expectedUser.getName(), actualUser.getName());
    }

}
