package today_store.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import today_store.authentication.dto.LoginRequest;
import today_store.authentication.dto.LoginResponse;
import today_store.authentication.dto.RefreshTokenRequest;
import today_store.authentication.dto.RefreshTokenResponse;
import today_store.authentication.exception.InvalidRefreshTokenException;
import today_store.authentication.service.AuthenticationService;
import today_store.common.ratelimit.RateLimitService;
import today_store.common.ratelimit.RateLimitTier;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("인증 컨트롤러 테스트")
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        given(rateLimitService.tryConsume(anyString(), any(RateLimitTier.class))).willReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("OAuth 로그인 성공 응답 반환")
    void shouldReturnLoginResponseWhenOauthLoginSucceeds() throws Exception {
        // OAuth 로그인에 성공하면 access/refresh token과 사용자 정보를 200 응답으로 반환해야 한다.

        // given
        UUID userId = UUID.randomUUID();
        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(LoginResponse.UserDetails.builder()
                        .id(userId)
                        .email("user@example.com")
                        .name("테스트 사용자")
                        .isFirstLogin(true)
                        .build())
                .build();
        given(authenticationService.oauthLogin(any(LoginRequest.class))).willReturn(response);
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "provider", "google",
                "code", "oauth-code"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);
        JsonNode firstLoginNode = body.get("user").has("firstLogin")
                ? body.get("user").get("firstLogin")
                : body.get("user").get("isFirstLogin");

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("accessToken").asText()).isEqualTo("access-token");
        assertThat(body.get("refreshToken").asText()).isEqualTo("refresh-token");
        assertThat(body.get("user").get("id").asText()).isEqualTo(userId.toString());
        assertThat(body.get("user").get("email").asText()).isEqualTo("user@example.com");
        assertThat(body.get("user").get("name").asText()).isEqualTo("테스트 사용자");
        assertThat(firstLoginNode).isNotNull();
        assertThat(firstLoginNode.asBoolean()).isTrue();
    }

    @Test
    @DisplayName("토큰 재발급 성공 응답 반환")
    void shouldReturnRefreshTokenResponseWhenRefreshSucceeds() throws Exception {
        // 리프레시 토큰 재발급에 성공하면 새 access/refresh token을 200 응답으로 반환해야 한다.

        // given
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();
        given(authenticationService.refreshToken(any(RefreshTokenRequest.class))).willReturn(response);
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "refreshToken", "valid-refresh-token"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
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
    @DisplayName("로그아웃 성공 시 204 반환")
    void shouldReturnNoContentWhenLogoutSucceeds() throws Exception {
        // 인증된 사용자가 올바른 bearer token으로 로그아웃하면 204 응답을 반환해야 한다.

        // given
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .principal(authenticationToken))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isBlank();
        then(authenticationService).should().logout("access-token", "user@example.com");
    }

    @Test
    @DisplayName("회원 삭제 성공 시 204 반환")
    void shouldReturnNoContentWhenDeleteSucceeds() throws Exception {
        // 인증된 사용자가 올바른 bearer token으로 회원 삭제를 요청하면 204 응답을 반환해야 한다.

        // given
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // when
        MvcResult result = mockMvc.perform(delete("/api/v1/auth/me")
                        .header("Authorization", "Bearer access-token")
                        .principal(authenticationToken))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isBlank();
        then(authenticationService).should().deleteUser("access-token", "user@example.com");
    }

    @Test
    @DisplayName("provider 누락 시 검증 오류 반환")
    void shouldReturnBadRequestWhenProviderIsBlank() throws Exception {
        // provider가 비어 있으면 공통 validation 에러 응답으로 400을 반환해야 한다.

        // given
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "provider", "",
                "code", "oauth-code"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("A001");
        assertThat(body.get("errors").size()).isEqualTo(1);
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("provider");
        assertThat(body.get("errors").get(0).get("reason").asText()).isEqualTo("소셜 로그인 제공자(provider)는 필수 항목입니다.");
    }

    @Test
    @DisplayName("code 누락 시 검증 오류 반환")
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        // code가 비어 있으면 공통 validation 에러 응답으로 400을 반환해야 한다.

        // given
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "provider", "google",
                "code", ""
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("A001");
        assertThat(body.get("errors").size()).isEqualTo(1);
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("code");
        assertThat(body.get("errors").get(0).get("reason").asText()).isEqualTo("인가 코드(code)는 필수 항목입니다.");
    }

    @Test
    @DisplayName("refreshToken 누락 시 검증 오류 반환")
    void shouldReturnBadRequestWhenRefreshTokenIsBlank() throws Exception {
        // refreshToken이 비어 있으면 공통 validation 에러 응답으로 400을 반환해야 한다.

        // given
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "refreshToken", ""
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("A001");
        assertThat(body.get("errors").size()).isEqualTo(1);
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("refreshToken");
        assertThat(body.get("errors").get(0).get("reason").asText()).isEqualTo("리프레시 토큰(refreshToken)은 필수 항목입니다.");
    }

    @Test
    @DisplayName("서비스 커스텀 예외를 공통 응답으로 반환")
    void shouldReturnCustomErrorResponseWhenServiceThrowsCustomException() throws Exception {
        // 서비스에서 CustomException이 발생하면 공통 에러 포맷으로 응답해야 한다.

        // given
        given(authenticationService.refreshToken(any(RefreshTokenRequest.class))).willThrow(new InvalidRefreshTokenException());
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "refreshToken", "invalid-refresh-token"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("A004");
        assertThat(body.get("message").asText()).isEqualTo("Invalid or expired refresh token.");
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 인증 오류 반환")
    void shouldReturnAuthenticationRequiredWhenAuthorizationHeaderIsNotBearer() throws Exception {
        // 인증 객체가 있어도 Authorization 헤더가 bearer 형식이 아니면 인증 오류를 반환해야 한다.

        // given
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Token access-token")
                        .principal(authenticationToken))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("A005");
        assertThat(body.get("message").asText()).isEqualTo("Full authentication is required.");
    }

    @Test
    @DisplayName("인증 객체가 없으면 인증 오류 반환")
    void shouldReturnAuthenticationRequiredWhenAuthenticationIsMissing() throws Exception {
        // Authorization 헤더가 있어도 인증 객체가 없으면 인증 오류를 반환해야 한다.

        // given

        // when
        MvcResult result = mockMvc.perform(delete("/api/v1/auth/me")
                        .header("Authorization", "Bearer access-token"))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("A005");
        assertThat(body.get("message").asText()).isEqualTo("Full authentication is required.");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
