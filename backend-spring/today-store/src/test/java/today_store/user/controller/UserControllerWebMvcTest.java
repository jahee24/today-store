package today_store.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import today_store.authentication.entity.User;
import today_store.common.exception.ValidationErrorResolver;
import today_store.common.ratelimit.RateLimitService;
import today_store.common.ratelimit.RateLimitTier;
import today_store.user.dto.UpdateUserProfileResponse;
import today_store.user.dto.UserProfileResponse;
import today_store.user.service.UserService;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ValidationErrorResolver.class)
@DisplayName("사용자 컨트롤러 테스트")
class UserControllerWebMvcTest {

    private static final String CLIENT_IP = "203.0.113.10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        given(rateLimitService.tryConsume(anyString(), any(RateLimitTier.class))).willReturn(true);
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    void shouldReturnMyProfile() throws Exception {
        // 인증된 사용자가 자신의 프로필을 조회하면 프로필 응답을 반환해야 한다.

        // given
        User user = createUser("user@example.com", "테스트 사용자");
        UUID userId = UUID.randomUUID();
        LocalDateTime lastLoginAt = LocalDateTime.of(2026, 3, 30, 14, 0);
        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("user@example.com")
                .name("테스트 사용자")
                .lastLoginAt(lastLoginAt)
                .build();
        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(userService.getUserProfile(user)).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("id").asText()).isEqualTo(userId.toString());
        assertThat(body.get("email").asText()).isEqualTo("user@example.com");
        assertThat(body.get("name").asText()).isEqualTo("테스트 사용자");
        assertThat(LocalDateTime.parse(body.get("lastLoginAt").asText())).isEqualTo(lastLoginAt);
        then(userService).should().getUser(any(Authentication.class));
        then(userService).should().getUserProfile(user);
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    void shouldUpdateMyProfile() throws Exception {
        // 인증된 사용자가 이름을 수정하면 수정 응답을 반환해야 한다.

        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 14, 10);
        UpdateUserProfileResponse response = UpdateUserProfileResponse.builder()
                .id(userId)
                .updatedAt(updatedAt)
                .build();
        given(userService.updateUserProfile(eq("user@example.com"), any())).willReturn(response);
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("name", "새 이름"));

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("id").asText()).isEqualTo(userId.toString());
        assertThat(LocalDateTime.parse(body.get("updatedAt").asText())).isEqualTo(updatedAt);
        then(userService).should().updateUserProfile(
                eq("user@example.com"),
                argThat(request -> request.getName().equals("새 이름"))
        );
    }

    @Test
    @DisplayName("내 프로필 수정 검증 오류")
    void shouldReturnBadRequestWhenNameIsInvalid() throws Exception {
        // 잘못된 이름 형식으로 수정 요청하면 U001 검증 에러를 반환해야 한다.

        // given
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("name", "a"));

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("U001");
        assertThat(body.get("message").asText()).isEqualTo("Invalid name format");
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("name");
    }

    @Test
    @DisplayName("내 프로필 수정 인증 오류")
    void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        // 인증 정보 없이 수정 요청하면 A005 인증 에러를 반환해야 한다.

        // given
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("name", "새 이름"));

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("A005");
        assertThat(body.get("message").asText()).isEqualTo("Full authentication is required.");
    }

    @Test
    @DisplayName("내 프로필 조회 요청 제한")
    void shouldReturnTooManyRequestsWhenGetProfileRateLimitIsExceeded() throws Exception {
        // 조회 요청 제한을 초과하면 R001 에러와 LOW 티어 호출을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.LOW)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        then(rateLimitService).should().tryConsume(CLIENT_IP, RateLimitTier.LOW);
    }

    @Test
    @DisplayName("내 프로필 수정 요청 제한")
    void shouldReturnTooManyRequestsWhenUpdateProfileRateLimitIsExceeded() throws Exception {
        // 수정 요청 제한을 초과하면 R001 에러와 MIDDLE 티어 호출을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("name", "새 이름"));

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        then(rateLimitService).should().tryConsume(CLIENT_IP, RateLimitTier.MIDDLE);
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UsernamePasswordAuthenticationToken authenticationToken() {
        return new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private User createUser(String email, String name) {
        return new User(email, name, "google", "provider-id", "https://image.test/profile.png");
    }
}
