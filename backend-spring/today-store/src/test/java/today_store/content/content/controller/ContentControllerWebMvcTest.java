package today_store.content.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import today_store.content.content.dto.ContentListResponse;
import today_store.content.content.dto.ContentResponse;
import today_store.content.content.dto.GenerateContentResponse;
import today_store.content.content.dto.TaskStatusResponse;
import today_store.content.content.entity.GenerationType;
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
    @DisplayName("콘텐츠 생성 시작 성공")
    void shouldGenerateContent() throws Exception {
        // 콘텐츠 생성 시작 요청이 들어오면 202 응답과 requestId, taskId, startedAt을 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.of(2026, 4, 14, 13, 0);
        GenerateContentResponse response = GenerateContentResponse.builder()
                .requestId(requestId)
                .taskId(taskId)
                .startedAt(startedAt)
                .build();

        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(contentService.startGeneration(user, requestId)).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/contents/{requestId}/generate", requestId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        assertThat(body.get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(body.get("taskId").asText()).isEqualTo(taskId.toString());
        assertThat(LocalDateTime.parse(body.get("startedAt").asText())).isEqualTo(startedAt);
        then(userService).should().getUser(any(Authentication.class));
        then(contentService).should().startGeneration(user, requestId);
    }

    @Test
    @DisplayName("작업 상태 조회 성공")
    void shouldReturnTaskStatus() throws Exception {
        // 작업 상태 조회 요청이 들어오면 snake_case 필드를 포함한 상태 응답을 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID taskId = UUID.randomUUID();
        TaskStatusResponse response = TaskStatusResponse.builder()
                .taskId(taskId)
                .status("error")
                .result(null)
                .errorMessage("AI generation request failed. Please try again later.")
                .build();

        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(contentService.getTaskStatus(user, taskId)).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/task/{apiLogId}", taskId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("task_id").asText()).isEqualTo(taskId.toString());
        assertThat(body.get("status").asText()).isEqualTo("error");
        assertThat(body.get("result").isNull()).isTrue();
        assertThat(body.get("error_message").asText()).isEqualTo("AI generation request failed. Please try again later.");
    }

    @Test
    @DisplayName("콘텐츠 목록 조회 성공")
    void shouldReturnContentList() throws Exception {
        // 요청별 콘텐츠 목록 조회 시 요약 정보와 생성 시각을 함께 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 14, 13, 10);
        ContentListResponse response = ContentListResponse.builder()
                .requestId(requestId)
                .contents(List.of(ContentListResponse.ContentSummary.builder()
                        .contentId(contentId)
                        .isPosted(true)
                        .instagramPreview("Instagram preview")
                        .karrotPreview("Karrot preview")
                        .naverPreview("Naver preview")
                        .createdAt(createdAt)
                        .build()))
                .build();

        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(contentService.getContentList(user, requestId)).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/{requestId}/contents", requestId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(body.get("contents").size()).isEqualTo(1);
        assertThat(body.get("contents").get(0).get("contentId").asText()).isEqualTo(contentId.toString());
        assertThat(body.get("contents").get(0).get("isPosted").asBoolean()).isTrue();
        assertThat(body.get("contents").get(0).get("instagramPreview").asText()).isEqualTo("Instagram preview");
        assertThat(LocalDateTime.parse(body.get("contents").get(0).get("createdAt").asText())).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("콘텐츠 상세 조회 성공")
    void shouldReturnContentDetail() throws Exception {
        // 콘텐츠 상세 조회 시 본문 데이터와 이미지 목록을 함께 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        UUID inputImageId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 14, 13, 20);
        LocalDateTime imageCreatedAt = LocalDateTime.of(2026, 4, 14, 13, 21);
        ContentResponse response = ContentResponse.builder()
                .id(contentId)
                .requestId(requestId)
                .generationType(GenerationType.ALL)
                .isPosted(true)
                .createdAt(createdAt)
                .contentData(ContentResponse.ContentData.builder()
                        .instagram(ContentResponse.PlatformData.builder().text("Instagram body").hashtags(List.of("#insta")).build())
                        .karrot(ContentResponse.PlatformData.builder().text("Karrot body").hashtags(List.of("#karrot")).build())
                        .naver(ContentResponse.PlatformData.builder().text("Naver body").hashtags(List.of("#naver")).build())
                        .build())
                .images(List.of(ContentResponse.ContentImageResponse.builder()
                        .id(imageId)
                        .inputImageId(inputImageId)
                        .url("https://signed.example.com/content-1")
                        .createdAt(imageCreatedAt)
                        .build()))
                .build();

        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(contentService.getContent(user, contentId)).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/{contentId}", contentId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("id").asText()).isEqualTo(contentId.toString());
        assertThat(body.get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(body.get("generationType").asText()).isEqualTo("ALL");
        assertThat(body.get("isPosted").asBoolean()).isTrue();
        assertThat(body.get("contentData").get("instagram").get("text").asText()).isEqualTo("Instagram body");
        assertThat(body.get("contentData").get("naver").get("hashtags").get(0).asText()).isEqualTo("#naver");
        assertThat(body.get("images").get(0).get("id").asText()).isEqualTo(imageId.toString());
        assertThat(body.get("images").get(0).get("inputImageId").asText()).isEqualTo(inputImageId.toString());
        assertThat(LocalDateTime.parse(body.get("images").get(0).get("createdAt").asText())).isEqualTo(imageCreatedAt);
    }

    @Test
    @DisplayName("콘텐츠 수정 성공")
    void shouldUpdateContent() throws Exception {
        // 콘텐츠 수정 요청이 들어오면 authentication 이름과 수정 본문을 서비스에 그대로 전달해야 한다.

        // given
        UUID contentId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 14, 13, 30);
        ContentResponse response = ContentResponse.builder()
                .id(contentId)
                .requestId(UUID.randomUUID())
                .generationType(GenerationType.ALL)
                .isPosted(false)
                .createdAt(createdAt)
                .contentData(ContentResponse.ContentData.builder()
                        .instagram(ContentResponse.PlatformData.builder().text("Updated instagram").hashtags(List.of("#new")).build())
                        .karrot(ContentResponse.PlatformData.builder().text("Karrot body").hashtags(List.of("#karrot")).build())
                        .naver(ContentResponse.PlatformData.builder().text("Updated naver").hashtags(List.of("#naver")).build())
                        .build())
                .images(List.of())
                .build();
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "instagram", Map.of("text", "Updated instagram", "hashtags", List.of("#new")),
                "naver", Map.of("text", "Updated naver")
        ));

        given(contentService.updateContent(eq("user@example.com"), eq(contentId), any())).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/contents/{contentId}", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("id").asText()).isEqualTo(contentId.toString());
        assertThat(body.get("contentData").get("instagram").get("text").asText()).isEqualTo("Updated instagram");
        assertThat(body.get("contentData").get("naver").get("text").asText()).isEqualTo("Updated naver");
        then(contentService).should().updateContent(
                eq("user@example.com"),
                eq(contentId),
                argThat(request ->
                        request.getInstagram() != null
                                && request.getInstagram().getText().equals("Updated instagram")
                                && request.getInstagram().getHashtags().equals(List.of("#new"))
                                && request.getNaver() != null
                                && request.getNaver().getText().equals("Updated naver"))
        );
        then(userService).should(never()).getUser(any(Authentication.class));
    }

    @Test
    @DisplayName("콘텐츠 삭제 성공")
    void shouldDeleteContent() throws Exception {
        // 콘텐츠 삭제 요청이 들어오면 204 응답과 함께 서비스 삭제 호출이 수행되어야 한다.

        // given
        User user = createUser("user@example.com");
        UUID contentId = UUID.randomUUID();
        given(userService.getUser(any(Authentication.class))).willReturn(user);

        // when
        MvcResult result = mockMvc.perform(delete("/api/v1/contents/{contentId}", contentId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isBlank();
        then(contentService).should().deleteContent(user, contentId);
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

    @Test
    @DisplayName("재생성 feedback 검증 오류")
    void shouldReturnBadRequestWhenFeedbackIsBlank() throws Exception {
        // feedback가 비어 있으면 C006 검증 오류를 반환해야 한다.

        // given
        UUID contentId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "feedback", "",
                "regenerateImage", false,
                "target", "NAVER"
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
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("feedback");
        then(userService).should(never()).getUser(any(Authentication.class));
    }

    @Test
    @DisplayName("콘텐츠 생성 시작 요청 제한")
    void shouldRateLimitGenerate() throws Exception {
        // 콘텐츠 생성 시작 엔드포인트는 HIGH 티어 요청 제한을 적용해야 한다.

        // given
        UUID requestId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.HIGH)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/contents/{requestId}/generate", requestId)
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(readBody(result).get("code").asText()).isEqualTo("R001");
    }

    @Test
    @DisplayName("작업 상태 조회 요청 제한")
    void shouldRateLimitTaskStatus() throws Exception {
        // 작업 상태 조회 엔드포인트는 LOW 티어 요청 제한을 적용해야 한다.

        // given
        UUID taskId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.LOW)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/task/{apiLogId}", taskId)
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(readBody(result).get("code").asText()).isEqualTo("R001");
    }

    @Test
    @DisplayName("콘텐츠 목록 조회 요청 제한")
    void shouldRateLimitContentList() throws Exception {
        // 콘텐츠 목록 조회 엔드포인트는 LOW 티어 요청 제한을 적용해야 한다.

        // given
        UUID requestId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.LOW)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/{requestId}/contents", requestId)
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(readBody(result).get("code").asText()).isEqualTo("R001");
    }

    @Test
    @DisplayName("콘텐츠 상세 조회 요청 제한")
    void shouldRateLimitContentDetail() throws Exception {
        // 콘텐츠 상세 조회 엔드포인트는 LOW 티어 요청 제한을 적용해야 한다.

        // given
        UUID contentId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.LOW)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/{contentId}", contentId)
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(readBody(result).get("code").asText()).isEqualTo("R001");
    }

    @Test
    @DisplayName("콘텐츠 수정 요청 제한")
    void shouldRateLimitContentUpdate() throws Exception {
        // 콘텐츠 수정 엔드포인트는 MIDDLE 티어 요청 제한을 적용해야 한다.

        // given
        UUID contentId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/contents/{contentId}", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(readBody(result).get("code").asText()).isEqualTo("R001");
    }

    @Test
    @DisplayName("콘텐츠 삭제 요청 제한")
    void shouldRateLimitContentDelete() throws Exception {
        // 콘텐츠 삭제 엔드포인트는 MIDDLE 티어 요청 제한을 적용해야 한다.

        // given
        UUID contentId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(delete("/api/v1/contents/{contentId}", contentId)
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(readBody(result).get("code").asText()).isEqualTo("R001");
    }

    @Test
    @DisplayName("콘텐츠 재생성 시작 요청 제한")
    void shouldRateLimitRegeneration() throws Exception {
        // 콘텐츠 재생성 시작 엔드포인트는 HIGH 티어 요청 제한을 적용해야 한다.

        // given
        UUID contentId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.HIGH)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/contents/{contentId}/regenerate", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"Retry\",\"target\":\"INSTAGRAM\"}")
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(readBody(result).get("code").asText()).isEqualTo("R001");
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
