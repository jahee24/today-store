package today_store.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import today_store.authentication.entity.User;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.user.entity.UserSocialAccount;
import today_store.user.repository.UserSocialAccountRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 소셜 계정 서비스 테스트")
class UserSocialAccountServiceTest {

    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;

    private UserSocialAccountService userSocialAccountService;

    @BeforeEach
    void setUp() {
        userSocialAccountService = new UserSocialAccountService(userSocialAccountRepository);
    }

    @Test
    @DisplayName("소셜 계정 최초 연결 시 계정 생성")
    void shouldCreateSocialAccountWhenNoExistingLinkExists() {
        // 최초 인스타그램 OAuth 연결은 요청 사용자에게 플랫폼 식별자,
        // 사용자명, 장기 토큰, 만료 시간, 유효 상태를 저장해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 1, 10, 0);

        given(userSocialAccountRepository.findByPlatformAndSocialUserId("INSTAGRAM", "ig-user-id"))
                .willReturn(Optional.empty());
        given(userSocialAccountRepository.findByUserAndPlatform(user, "INSTAGRAM"))
                .willReturn(Optional.empty());

        // when
        userSocialAccountService.linkAccount(
                user,
                "INSTAGRAM",
                "ig-user-id",
                "today_store",
                "long-lived-token",
                expiresAt
        );

        // then
        ArgumentCaptor<UserSocialAccount> captor = ArgumentCaptor.forClass(UserSocialAccount.class);
        then(userSocialAccountRepository).should().save(captor.capture());
        UserSocialAccount savedAccount = captor.getValue();
        assertThat(savedAccount.getUser()).isSameAs(user);
        assertThat(savedAccount.getPlatform()).isEqualTo("INSTAGRAM");
        assertThat(savedAccount.getSocialUserId()).isEqualTo("ig-user-id");
        assertThat(savedAccount.getUsername()).isEqualTo("today_store");
        assertThat(savedAccount.getAccessToken()).isEqualTo("long-lived-token");
        assertThat(savedAccount.getTokenExpiresAt()).isEqualTo(expiresAt);
        assertThat(savedAccount.getIsValid()).isTrue();
    }

    @Test
    @DisplayName("동일 사용자 계정 재연결 시 기존 계정 갱신")
    void shouldUpdateExistingAccountForSameUserAndPlatform() {
        // 같은 사용자의 인스타그램 계정을 다시 인증하면 중복 행을 만들지 않고
        // 기존 계정의 식별자, 사용자명, 토큰, 만료 시간, 유효 상태를 갱신해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        UserSocialAccount existingAccount = new UserSocialAccount();
        existingAccount.setUser(user);
        existingAccount.setPlatform("INSTAGRAM");
        existingAccount.setSocialUserId("old-ig-user-id");
        existingAccount.setUsername("old_username");
        existingAccount.setAccessToken("old-token");
        existingAccount.setTokenExpiresAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        existingAccount.setIsValid(false);
        LocalDateTime newExpiresAt = LocalDateTime.of(2026, 6, 1, 10, 0);

        given(userSocialAccountRepository.findByPlatformAndSocialUserId("INSTAGRAM", "ig-user-id"))
                .willReturn(Optional.empty());
        given(userSocialAccountRepository.findByUserAndPlatform(user, "INSTAGRAM"))
                .willReturn(Optional.of(existingAccount));

        // when
        userSocialAccountService.linkAccount(
                user,
                "INSTAGRAM",
                "ig-user-id",
                "today_store",
                "new-long-lived-token",
                newExpiresAt
        );

        // then
        then(userSocialAccountRepository).should().save(existingAccount);
        assertThat(existingAccount.getSocialUserId()).isEqualTo("ig-user-id");
        assertThat(existingAccount.getUsername()).isEqualTo("today_store");
        assertThat(existingAccount.getAccessToken()).isEqualTo("new-long-lived-token");
        assertThat(existingAccount.getTokenExpiresAt()).isEqualTo(newExpiresAt);
        assertThat(existingAccount.getIsValid()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 소셜 계정 연결 거부")
    void shouldRejectSocialAccountAlreadyLinkedToAnotherUser() {
        // 하나의 인스타그램 식별자가 두 사용자에게 연결되면
        // 다른 사용자의 연결 계정으로 게시할 수 있으므로 반드시 거부해야 한다.

        // given
        User requester = createUser(UUID.randomUUID(), "requester@example.com");
        User otherUser = createUser(UUID.randomUUID(), "other@example.com");
        UserSocialAccount existingAccount = new UserSocialAccount();
        existingAccount.setUser(otherUser);
        existingAccount.setPlatform("INSTAGRAM");
        existingAccount.setSocialUserId("ig-user-id");

        given(userSocialAccountRepository.findByPlatformAndSocialUserId("INSTAGRAM", "ig-user-id"))
                .willReturn(Optional.of(existingAccount));

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> userSocialAccountService.linkAccount(
                        requester,
                        "INSTAGRAM",
                        "ig-user-id",
                        "today_store",
                        "long-lived-token",
                        LocalDateTime.of(2026, 5, 1, 10, 0)
                )
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSTAGRAM_ALREADY_LINKED);
        then(userSocialAccountRepository).should(never()).findByUserAndPlatform(any(User.class), any());
        then(userSocialAccountRepository).should(never()).save(any(UserSocialAccount.class));
    }

    private User createUser(UUID id, String email) {
        User user = new User(email, "Test User", "google", id.toString(), "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
