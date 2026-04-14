package today_store.content.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import today_store.authentication.entity.User;
import today_store.common.exception.ValidationErrorResolver;
import today_store.common.ratelimit.RateLimitService;
import today_store.common.ratelimit.RateLimitTier;
import today_store.content.content.dto.GenerateContentResponse;
import today_store.content.content.service.ContentService;
import today_store.user.service.UserService;

@WebMvcTest(controllers = ContentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ValidationErrorResolver.class)
@DisplayName("콘텐츠 컨트롤러 테스트")
class ContentControllerWebMvcTest {

    private static final String CLIENT_IP = "203.0.113.30";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        given(rateLimitService.tryConsume(anyString(), any(RateLimitTier.class))).willReturn(true);
    }

    @Test
    @DisplayName("콘텐츠 재생성 성공")
    void shouldRegenerateContentWhenRequestIsValid() throws Exception {
        // 유효한 재생성 요청이 오면 202 응답과 함께 생성 요청 ID, 작업 ID, 시작 시각을 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID contentId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.of(2026, 4, 13, 15, 0);
        GenerateContentResponse response = GenerateContentResponse.builder()
                .requestId(requestId)
                .taskId(taskId)
                .startedAt(startedAt)
                .build();
        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(contentService.startRegeneration(eq(user), eq(contentId), any())).willReturn(response);
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "feedback", "톤을 조금 더 친근하게 바꿔줘",
                "regenerateImage", true,
                "target", "INSTAGRAM"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/contents/{contentId}/regenerate", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        assertThat(body.get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(body.get("taskId").asText()).isEqualTo(taskId.toString());
        then(userService).should().getUser(any(Authentication.class));
        then(contentService).should().startRegeneration(
                eq(user),
                eq(contentId),
                argThat(request ->
                        request.getFeedback().equals("톤을 조금 더 친근하게 바꿔줘")
                                && request.isRegenerateImage()
                                && request.getTarget().equals("INSTAGRAM"))
        );
    }

    @Test
    @DisplayName("재생성 target 검증 실패")
    void shouldReturnBadRequestWhenTargetPlatformIsInvalid() throws Exception {
        // 허용되지 않은 target 값으로 재생성을 요청하면 C006 검증 오류와 함께 400 응답을 반환해야 한다.

        // given
        UUID contentId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "feedback", "문구를 수정해줘",
                "regenerateImage", false,
                "target", "YOUTUBE"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/contents/{contentId}/regenerate", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("C006");
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("target");
        then(userService).should(never()).getUser(any(Authentication.class));
        then(contentService).should(never()).startRegeneration(any(), any(), any());
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UsernamePasswordAuthenticationToken authenticationToken() {
        return new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private User createUser(String email) {
        User user = new User(email, "테스트 사용자", "google", UUID.randomUUID().toString(), "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
