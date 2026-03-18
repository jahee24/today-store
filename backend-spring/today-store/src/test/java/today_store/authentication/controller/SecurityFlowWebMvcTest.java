package today_store.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import today_store.authentication.dto.LoginRequest;
import today_store.authentication.dto.LoginResponse;
import today_store.authentication.dto.RefreshTokenRequest;
import today_store.authentication.dto.RefreshTokenResponse;
import today_store.authentication.jwt.JwtAccessDeniedHandler;
import today_store.authentication.jwt.JwtAuthenticationEntryPoint;
import today_store.authentication.jwt.JwtTokenProvider;
import today_store.authentication.repository.BlacklistedTokenRepository;
import today_store.authentication.service.AuthenticationService;
import today_store.common.config.SecurityConfig;
import today_store.common.ratelimit.RateLimitService;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
@DisplayName("인증 보안 흐름 테스트")
class SecurityFlowWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @MockitoBean
    private RateLimitService rateLimitService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("로그인 엔드포인트는 비인증 접근 가능")
    void shouldAllowOauthLoginWithoutAuthentication() throws Exception {
        // 로그인 엔드포인트는 인증 없이 접근 가능해야 하며 정상 응답을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume("203.0.113.10", today_store.common.ratelimit.RateLimitTier.HIGH)).willReturn(true);
        given(authenticationService.oauthLogin(any(LoginRequest.class))).willReturn(LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(LoginResponse.UserDetails.builder()
                        .email("user@example.com")
                        .name("테스트 사용자")
                        .isFirstLogin(false)
                        .build())
                .build());
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "provider", "google",
                "code", "oauth-code"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("accessToken").asText()).isEqualTo("access-token");
        assertThat(body.get("refreshToken").asText()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("재발급 엔드포인트는 비인증 접근 가능")
    void shouldAllowRefreshWithoutAuthentication() throws Exception {
        // 재발급 엔드포인트는 인증 없이 접근 가능해야 하며 정상 응답을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume("203.0.113.11", today_store.common.ratelimit.RateLimitTier.HIGH)).willReturn(true);
        given(authenticationService.refreshToken(any(RefreshTokenRequest.class))).willReturn(
                RefreshTokenResponse.builder()
                        .accessToken("new-access-token")
                        .refreshToken("new-refresh-token")
                        .build()
        );
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "refreshToken", "valid-refresh-token"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Forwarded-For", "203.0.113.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("accessToken").asText()).isEqualTo("new-access-token");
        assertThat(body.get("refreshToken").asText()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("로그아웃 엔드포인트는 인증 필요")
    void shouldReturnUnauthorizedWhenLogoutIsRequestedWithoutAuthentication() throws Exception {
        // 보호된 로그아웃 엔드포인트는 인증 정보가 없으면 401 응답을 반환해야 한다.

        // given

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout"))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("A005");
        assertThat(body.get("message").asText()).isEqualTo("Full authentication is required.");
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 로그아웃 성공")
    void shouldAllowLogoutWhenBearerTokenIsValid() throws Exception {
        // 유효한 bearer token이 전달되면 인증이 설정되고 로그아웃 요청이 정상 처리되어야 한다.

        // given
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        given(blacklistedTokenRepository.existsById("valid-token")).willReturn(false);
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getAuthentication("valid-token")).willReturn(authenticationToken);
        given(rateLimitService.tryConsume("user@example.com", today_store.common.ratelimit.RateLimitTier.HIGH)).willReturn(true);

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer valid-token"))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isBlank();
        then(authenticationService).should().logout("valid-token", "user@example.com");
    }

    @Test
    @DisplayName("블랙리스트 토큰이면 인증 실패")
    void shouldReturnUnauthorizedWhenTokenIsBlacklisted() throws Exception {
        // 블랙리스트에 등록된 토큰은 인증되지 않아 401 응답을 반환해야 한다.

        // given
        given(blacklistedTokenRepository.existsById("blacklisted-token")).willReturn(true);

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer blacklisted-token"))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("A005");
        then(authenticationService).should(never()).logout(anyString(), anyString());
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증 실패")
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        // 유효하지 않은 JWT가 전달되면 인증되지 않아 401 응답을 반환해야 한다.

        // given
        given(blacklistedTokenRepository.existsById("invalid-token")).willReturn(false);
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer invalid-token"))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("A005");
        then(authenticationService).should(never()).logout(anyString(), anyString());
    }

    @Test
    @DisplayName("레이트리밋 초과 시 429 반환")
    void shouldReturnTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
        // 레이트리밋을 초과한 로그인 요청은 공통 에러 포맷의 429 응답을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume("203.0.113.20", today_store.common.ratelimit.RateLimitTier.HIGH)).willReturn(false);
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "provider", "google",
                "code", "oauth-code"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .header("X-Forwarded-For", "203.0.113.20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        assertThat(body.get("message").asText()).isEqualTo("Rate limit exceeded. Please try again later.");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
