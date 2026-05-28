package today_store.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import today_store.authentication.entity.RefreshToken;
import today_store.authentication.entity.User;
import today_store.authentication.jwt.JwtTokenProvider;
import today_store.authentication.repository.RefreshTokenRepository;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.user.dto.UpdateUserProfileRequest;
import today_store.user.dto.UpdateUserProfileResponse;
import today_store.user.dto.UserProfileResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 서비스 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, jwtTokenProvider, refreshTokenRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보로 사용자 조회")
    void shouldReturnUserWhenAuthenticationIsValid() {
        // 인증 객체에 담긴 이메일로 사용자를 조회해야 한다.

        // given
        User user = createUser("user@example.com", "테스트 사용자");
        Authentication authentication = new UsernamePasswordAuthenticationToken("user@example.com", null);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

        // when
        User result = userService.getUser(authentication);

        // then
        assertThat(result).isSameAs(user);
        then(userRepository).should().findByEmail("user@example.com");
    }

    @Test
    @DisplayName("인증 정보 없는 사용자 조회 실패")
    void shouldThrowWhenAuthenticationIsMissing() {
        // 인증 객체가 없으면 인증 필요 예외를 반환해야 한다.

        // given

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> userService.getUser(null)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 실패")
    void shouldThrowWhenUserDoesNotExist() {
        // 인증 이메일에 해당하는 사용자가 없으면 사용자 없음 예외를 반환해야 한다.

        // given
        Authentication authentication = new UsernamePasswordAuthenticationToken("missing@example.com", null);
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> userService.getUser(authentication)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("사용자 프로필 응답 매핑")
    void shouldMapUserToUserProfileResponse() {
        // 사용자 엔티티를 프로필 응답 DTO로 변환해야 한다.

        // given
        LocalDateTime lastLoginAt = LocalDateTime.of(2026, 3, 30, 10, 15);
        User user = createUser("user@example.com", "테스트 사용자");
        ReflectionTestUtils.setField(user, "lastLoginAt", lastLoginAt);

        // when
        UserProfileResponse response = userService.getUserProfile(user);

        // then
        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getName()).isEqualTo("테스트 사용자");
        assertThat(response.getLastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    @DisplayName("사용자 프로필 수정")
    void shouldUpdateUserProfile() {
        // 이메일 변경 없이 이름만 수정하면 기존 프로필 수정 계약을 유지하고 토큰을 재발급하지 않아야 한다.

        // given
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 11, 20);
        User user = createUser("user@example.com", "기존 이름");
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("새 이름")
                .build();
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(userRepository.saveAndFlush(user)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(user, "updatedAt", updatedAt);
            return user;
        });

        // when
        UpdateUserProfileResponse response = userService.updateUserProfile("user@example.com", request);

        // then
        assertThat(user.getName()).isEqualTo("새 이름");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getName()).isEqualTo("새 이름");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        then(userRepository).should().saveAndFlush(user);
        then(userRepository).should(never()).existsByEmail(any());
        then(jwtTokenProvider).should(never()).createAccessToken(any());
        then(jwtTokenProvider).should(never()).createRefreshToken(any());
        then(refreshTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("프로필 이메일 변경")
    void shouldUpdateEmailAndReissueTokensWhenEmailChanges() {
        // 인증 주체로 쓰이는 이메일이 변경되면 중복 검증 후 새 이메일 기준 토큰을 재발급하고 Redis Refresh Token을 갱신해야 한다.

        // given
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 11, 30);
        User user = createUser("old@example.com", "기존 이름");
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("새 이름")
                .email("new@example.com")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "old@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
        given(userRepository.findByEmail("old@example.com")).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(userRepository.saveAndFlush(user)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(user, "updatedAt", updatedAt);
            return user;
        });
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn("new-refresh-token");

        // when
        UpdateUserProfileResponse response = userService.updateUserProfile("old@example.com", request);

        // then
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getName()).isEqualTo("새 이름");
        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getName()).isEqualTo("새 이름");
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        then(jwtTokenProvider).should().createAccessToken(authenticationCaptor.capture());
        then(jwtTokenProvider).should().createRefreshToken(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getAllValues())
                .extracting(Authentication::getName)
                .containsOnly("new@example.com");
        assertThat(authenticationCaptor.getAllValues().get(0).getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should().save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(refreshTokenCaptor.getValue().getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("중복 이메일 변경 실패")
    void shouldThrowWhenUpdatedEmailAlreadyExists() {
        // 이미 사용 중인 이메일로 변경하려 하면 사용자 정보 저장과 토큰 재발급 없이 U002 예외를 반환해야 한다.

        // given
        User user = createUser("old@example.com", "기존 이름");
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("새 이름")
                .email("duplicate@example.com")
                .build();
        given(userRepository.findByEmail("old@example.com")).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("duplicate@example.com")).willReturn(true);

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> userService.updateUserProfile("old@example.com", request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
        assertThat(user.getEmail()).isEqualTo("old@example.com");
        then(userRepository).should(never()).saveAndFlush(any());
        then(jwtTokenProvider).should(never()).createAccessToken(any());
        then(jwtTokenProvider).should(never()).createRefreshToken(any());
        then(refreshTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 프로필 수정 실패")
    void shouldThrowWhenUpdatingMissingUser() {
        // 수정 대상 사용자가 없으면 사용자 없음 예외를 반환해야 한다.

        // given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("새 이름")
                .build();
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> userService.updateUserProfile("missing@example.com", request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    private User createUser(String email, String name) {
        User user = new User(email, name, "google", "provider-id", "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.of(2026, 3, 30, 9, 0));
        return user;
    }
}
