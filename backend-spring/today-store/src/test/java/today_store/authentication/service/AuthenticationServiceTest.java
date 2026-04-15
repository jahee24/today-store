package today_store.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import today_store.authentication.dto.LoginRequest;
import today_store.authentication.dto.LoginResponse;
import today_store.authentication.dto.RefreshTokenRequest;
import today_store.authentication.dto.RefreshTokenResponse;
import today_store.authentication.entity.BlacklistedToken;
import today_store.authentication.entity.RefreshToken;
import today_store.authentication.entity.User;
import today_store.authentication.exception.InvalidOauthAccessTokenException;
import today_store.authentication.exception.InvalidRefreshTokenException;
import today_store.authentication.exception.UnsupportedProviderException;
import today_store.authentication.exception.UserDisabledException;
import today_store.authentication.exception.UserNotFoundException;
import today_store.authentication.jwt.JwtTokenProvider;
import today_store.authentication.repository.BlacklistedTokenRepository;
import today_store.authentication.repository.RefreshTokenRepository;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스 테스트")
class AuthenticationServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = createService(createUnexpectedCallWebClient());
    }

    @Test
    @DisplayName("신규 사용자의 OAuth 로그인 처리")
    void shouldReturnLoginResponseWithFirstLoginWhenOauthLoginForNewUser() {
        // 신규 사용자가 OAuth 로그인에 성공하면 토큰을 발급하고 첫 로그인 상태로 응답해야 한다.

        // given
        authenticationService = createService(createWebClient(
                jsonResponse(HttpStatus.OK, """
                        {"sub":"provider-id","name":"새 사용자","email":"newuser@example.com","picture":"https://image.test/profile.png"}
                        """)
        ));
        ClientRegistration clientRegistration = createGoogleClientRegistration();
        UUID savedUserId = UUID.randomUUID();
        LoginRequest loginRequest = new LoginRequest("google", "valid-access-token");

        given(clientRegistrationRepository.findByRegistrationId("google")).willReturn(clientRegistration);
        given(userRepository.findByProviderAndProviderId("google", "provider-id")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", savedUserId);
            return user;
        });
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn("refresh-token");

        // when
        LoginResponse response = authenticationService.oauthLogin(loginRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getId()).isEqualTo(savedUserId);
        assertThat(response.getUser().getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getUser().getName()).isEqualTo("새 사용자");
        assertThat(response.getUser().isFirstLogin()).isTrue();
        then(userRepository).should().save(argThat(user ->
                user.getEmail().equals("newuser@example.com")
                        && user.getProvider().equals("google")
                        && user.getProviderId().equals("provider-id")
                        && user.getProfileImageUrl().equals("https://image.test/profile.png")
        ));
        then(refreshTokenRepository).should().save(argThat(token ->
                token.getUserId().equals(savedUserId)
                        && token.getRefreshToken().equals("refresh-token")
        ));
    }

    @Test
    @DisplayName("기존 사용자의 OAuth 로그인 처리")
    void shouldReturnLoginResponseWithExistingUserWhenOauthLoginForExistingUser() {
        // 기존 활성 사용자가 OAuth 로그인에 성공하면 마지막 로그인 시간을 갱신하고 첫 로그인 아님으로 응답해야 한다.

        // given
        authenticationService = createService(createWebClient(
                jsonResponse(HttpStatus.OK, """
                        {"sub":"provider-id","name":"기존 사용자","email":"existing@example.com","picture":"https://image.test/profile.png"}
                        """)
        ));
        ClientRegistration clientRegistration = createGoogleClientRegistration();
        User existingUser = createUser("existing@example.com", "provider-id", true);
        LoginRequest loginRequest = new LoginRequest("google", "valid-access-token");

        given(clientRegistrationRepository.findByRegistrationId("google")).willReturn(clientRegistration);
        given(userRepository.findByProviderAndProviderId("google", "provider-id")).willReturn(Optional.of(existingUser));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn("refresh-token");

        // when
        LoginResponse response = authenticationService.oauthLogin(loginRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getId()).isEqualTo(existingUser.getId());
        assertThat(response.getUser().isFirstLogin()).isFalse();
        assertThat(existingUser.getLastLoginAt()).isNotNull();
        then(refreshTokenRepository).should().save(argThat(token ->
                token.getUserId().equals(existingUser.getId())
                        && token.getRefreshToken().equals("refresh-token")
        ));
    }

    @Test
    @DisplayName("지원하지 않는 소셜 제공자 거부")
    void shouldThrowUnsupportedProviderExceptionWhenProviderIsUnsupported() {
        // 등록되지 않은 OAuth 제공자로 로그인하면 지원하지 않는 제공자 예외를 반환해야 한다.

        // given
        LoginRequest loginRequest = new LoginRequest("naver", "valid-access-token");
        given(clientRegistrationRepository.findByRegistrationId("naver")).willReturn(null);

        // when
        UnsupportedProviderException exception = assertThrows(
                UnsupportedProviderException.class,
                () -> authenticationService.oauthLogin(loginRequest)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER.getMessage());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("OAuth 사용자 정보 조회 실패 예외 처리")
    void shouldThrowInvalidOauthAccessTokenExceptionWhenUserInfoLookupFails() {
        // OAuth 토큰으로 사용자 정보 조회가 실패하면 액세스 토큰 오류 예외를 반환해야 한다.

        // given
        authenticationService = createService(createWebClient(
                jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, """
                        {"error":"provider_error"}
                        """)
        ));
        LoginRequest loginRequest = new LoginRequest("google", "invalid-access-token");
        given(clientRegistrationRepository.findByRegistrationId("google")).willReturn(createGoogleClientRegistration());

        // when
        InvalidOauthAccessTokenException exception = assertThrows(
                InvalidOauthAccessTokenException.class,
                () -> authenticationService.oauthLogin(loginRequest)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OAUTH_TOKEN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_OAUTH_TOKEN.getMessage());
        then(userRepository).should(never()).findByProviderAndProviderId(anyString(), anyString());
    }

    @Test
    @DisplayName("비활성 사용자의 로그인 차단")
    void shouldThrowUserDisabledExceptionWhenExistingUserIsInactive() {
        // 비활성화된 사용자가 다시 로그인하면 계정 비활성 예외를 반환해야 한다.

        // given
        authenticationService = createService(createWebClient(
                jsonResponse(HttpStatus.OK, """
                        {"sub":"provider-id","name":"비활성 사용자","email":"inactive@example.com","picture":"https://image.test/profile.png"}
                        """)
        ));
        User inactiveUser = createUser("inactive@example.com", "provider-id", false);
        LoginRequest loginRequest = new LoginRequest("google", "valid-access-token");

        given(clientRegistrationRepository.findByRegistrationId("google")).willReturn(createGoogleClientRegistration());
        given(userRepository.findByProviderAndProviderId("google", "provider-id")).willReturn(Optional.of(inactiveUser));

        // when
        UserDisabledException exception = assertThrows(
                UserDisabledException.class,
                () -> authenticationService.oauthLogin(loginRequest)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_DISABLED.getMessage());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 재발급")
    void shouldRotateTokensWhenRefreshTokenIsValid() {
        // 유효한 리프레시 토큰이 저장소 값과 일치하면 access/refresh token을 모두 재발급해야 한다.

        // given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        User user = createUser("refresh@example.com", "provider-id", true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "refresh@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(userRepository.findByEmail("refresh@example.com")).willReturn(Optional.of(user));
        given(refreshTokenRepository.findById(user.getId())).willReturn(Optional.of(new RefreshToken(user.getId(), refreshToken)));
        given(jwtTokenProvider.createAccessToken(authentication)).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(authentication)).willReturn("new-refresh-token");

        // when
        RefreshTokenResponse response = authenticationService.refreshToken(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        then(refreshTokenRepository).should().save(argThat(token ->
                token.getUserId().equals(user.getId())
                        && token.getRefreshToken().equals("new-refresh-token")
        ));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰 거부")
    void shouldThrowInvalidRefreshTokenExceptionWhenRefreshTokenIsInvalid() {
        // JWT 자체가 유효하지 않은 리프레시 토큰이면 즉시 예외를 반환해야 한다.

        // given
        String refreshToken = "invalid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(false);

        // when
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authenticationService.refreshToken(request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
        then(userRepository).should(never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("저장된 리프레시 토큰이 없으면 재발급 거부")
    void shouldThrowInvalidRefreshTokenExceptionWhenStoredRefreshTokenDoesNotExist() {
        // JWT는 유효하지만 저장소에 리프레시 토큰이 없으면 재발급을 거부해야 한다.

        // given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        User user = createUser("refresh@example.com", "provider-id", true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "refresh@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(userRepository.findByEmail("refresh@example.com")).willReturn(Optional.of(user));
        given(refreshTokenRepository.findById(user.getId())).willReturn(Optional.empty());

        // when
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authenticationService.refreshToken(request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
        then(refreshTokenRepository).should().findById(user.getId());
    }

    @Test
    @DisplayName("저장된 리프레시 토큰이 다르면 재발급 거부")
    void shouldThrowInvalidRefreshTokenExceptionWhenStoredRefreshTokenDoesNotMatch() {
        // 저장소에 있는 리프레시 토큰과 요청 토큰이 다르면 재발급을 거부해야 한다.

        // given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        User user = createUser("refresh@example.com", "provider-id", true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "refresh@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(refreshToken)).willReturn(authentication);
        given(userRepository.findByEmail("refresh@example.com")).willReturn(Optional.of(user));
        given(refreshTokenRepository.findById(user.getId())).willReturn(Optional.of(new RefreshToken(user.getId(), "other-refresh-token")));

        // when
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authenticationService.refreshToken(request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
        then(refreshTokenRepository).should(never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그아웃 시 블랙리스트와 리프레시 토큰 정리")
    void shouldBlacklistAccessTokenAndDeleteRefreshTokenWhenLogout() {
        // 로그아웃 요청이 들어오면 access token을 블랙리스트에 등록하고 저장된 리프레시 토큰을 삭제해야 한다.

        // given
        User user = createUser("logout@example.com", "provider-id", true);
        given(jwtTokenProvider.getRemainingExpirationMillis("access-token")).willReturn(120L);
        given(userRepository.findByEmail("logout@example.com")).willReturn(Optional.of(user));

        // when
        authenticationService.logout("access-token", "logout@example.com");

        // then
        then(blacklistedTokenRepository).should().save(argThat(token ->
                token.getToken().equals("access-token")
                        && token.getStatus().equals("logout")
                        && token.getTimeToLive().equals(120L)
        ));
        then(refreshTokenRepository).should().deleteById(user.getId());
    }

    @Test
    @DisplayName("회원 탈퇴 시 사용자 비활성화와 토큰 정리 수행")
    void shouldDeactivateUserAndCleanupTokensWhenDeleteUser() {
        // 활성 사용자가 탈퇴하면 사용자를 비활성화하고 refresh token 삭제 및 access token 블랙리스트 등록을 수행해야 한다.

        // given
        User user = createUser("delete@example.com", "provider-id", true);
        given(userRepository.findByEmail("delete@example.com")).willReturn(Optional.of(user));
        given(jwtTokenProvider.getRemainingExpirationMillis("access-token")).willReturn(60L);

        // when
        authenticationService.deleteUser("access-token", "delete@example.com");

        // then
        assertThat(user.getIsActive()).isFalse();
        then(refreshTokenRepository).should().deleteById(user.getId());
        then(blacklistedTokenRepository).should().save(argThat(token ->
                token.getToken().equals("access-token")
                        && token.getStatus().equals("delete_account")
                        && token.getTimeToLive().equals(60L)
        ));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 탈퇴 거부")
    void shouldThrowUserNotFoundExceptionWhenDeletingUnknownUser() {
        // 탈퇴 대상 사용자가 존재하지 않으면 사용자 없음 예외를 반환해야 한다.

        // given
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authenticationService.deleteUser("access-token", "missing@example.com")
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
        then(refreshTokenRepository).should(never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("이미 비활성화된 사용자의 탈퇴 거부")
    void shouldThrowUserDisabledExceptionWhenDeletingInactiveUser() {
        // 이미 비활성화된 사용자가 다시 탈퇴를 시도하면 계정 비활성 예외를 반환해야 한다.

        // given
        User inactiveUser = createUser("inactive@example.com", "provider-id", false);
        given(userRepository.findByEmail("inactive@example.com")).willReturn(Optional.of(inactiveUser));

        // when
        UserDisabledException exception = assertThrows(
                UserDisabledException.class,
                () -> authenticationService.deleteUser("access-token", "inactive@example.com")
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_DISABLED.getMessage());
        then(refreshTokenRepository).should(never()).deleteById(any(UUID.class));
        then(blacklistedTokenRepository).should(never()).save(any(BlacklistedToken.class));
    }

    private AuthenticationService createService(WebClient webClient) {
        return new AuthenticationService(
                jwtTokenProvider,
                userRepository,
                clientRegistrationRepository,
                refreshTokenRepository,
                blacklistedTokenRepository,
                webClient
        );
    }

    private WebClient createUnexpectedCallWebClient() {
        return WebClient.builder()
                .exchangeFunction(request -> Mono.error(new IllegalStateException("Unexpected WebClient call: " + request.url())))
                .build();
    }

    private WebClient createWebClient(ClientResponse... responses) {
        Queue<ClientResponse> queue = new ArrayDeque<>(List.of(responses));
        ExchangeFunction exchangeFunction = request -> {
            ClientResponse response = queue.poll();
            if (response == null) {
                return Mono.error(new IllegalStateException("No stubbed response for request: " + request.url()));
            }
            return Mono.just(response);
        };
        return WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
    }

    private ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private ClientRegistration createGoogleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("google-client-id")
                .clientSecret("google-client-secret")
                .redirectUri("http://localhost/test/oauth/google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope("profile", "email")
                .authorizationUri("https://example.test/oauth/authorize")
                .tokenUri("https://example.test/oauth/token")
                .userInfoUri("https://example.test/user/me")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
    }

    private User createUser(String email, String providerId, boolean active) {
        User user = new User(
                email,
                "테스트 사용자",
                "google",
                providerId,
                "https://image.test/profile.png"
        );
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        if (!active) {
            user.deactivate();
        }
        return user;
    }
}
