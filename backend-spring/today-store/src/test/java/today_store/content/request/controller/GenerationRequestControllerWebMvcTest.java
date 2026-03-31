package today_store.content.request.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
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
import today_store.content.request.dto.CreateGenerationResponse;
import today_store.content.request.dto.GenerationRequestDetailResponse;
import today_store.content.request.dto.GenerationRequestListResponse;
import today_store.content.request.dto.UpdateGenerationResponse;
import today_store.content.request.service.GenerationRequestService;
import today_store.user.service.UserService;

@WebMvcTest(controllers = GenerationRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ValidationErrorResolver.class)
@DisplayName("생성 요청 컨트롤러 테스트")
class GenerationRequestControllerWebMvcTest {

    private static final String CLIENT_IP = "203.0.113.30";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GenerationRequestService generationRequestService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        given(rateLimitService.tryConsume(anyString(), any(RateLimitTier.class))).willReturn(true);
    }

    @Test
    @DisplayName("생성 요청 생성 성공")
    void shouldCreateGenerationRequest() throws Exception {
        // 멀티파트 생성 요청이 들어오면 201 응답과 생성 정보를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 18, 0);
        MockMultipartFile image = createMultipartFile("images", "first.png", "first-image");
        CreateGenerationResponse response = CreateGenerationResponse.builder()
                .requestId(requestId)
                .createdAt(createdAt)
                .build();
        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(generationRequestService.createRequest(eq(user), any())).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(multipart("/api/v1/contents/request")
                        .file(image)
                        .param("concept", "봄 프로모션")
                        .param("additionalNote", "따뜻한 분위기")
                        .param("targetAge", "20대")
                        .param("targetGender", "여성")
                        .param("imageDescriptions", "정면 이미지")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(body.get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(LocalDateTime.parse(body.get("createdAt").asText())).isEqualTo(createdAt);
        then(userService).should().getUser(any(Authentication.class));
        then(generationRequestService).should().createRequest(
                eq(user),
                argThat(request ->
                        request.getConcept().equals("봄 프로모션")
                                && request.getAdditionalNote().equals("따뜻한 분위기")
                                && request.getTargetAge().equals("20대")
                                && request.getTargetGender().equals("여성")
                                && request.getImageDescriptions().equals(List.of("정면 이미지"))
                                && request.getImages().size() == 1
                )
        );
    }

    @Test
    @DisplayName("생성 요청 목록 조회 성공")
    void shouldReturnGenerationRequestList() throws Exception {
        // 인증된 사용자가 요청 목록을 조회하면 목록과 페이징 정보를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 18, 10);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 18, 20);
        GenerationRequestListResponse response = GenerationRequestListResponse.builder()
                .data(List.of(
                        GenerationRequestListResponse.GenerationRequestSummary.builder()
                                .requestId(requestId)
                                .concept("봄 프로모션")
                                .thumbnailUrl("https://signed.example.com/thumb")
                                .imageCount(2)
                                .createdAt(createdAt)
                                .updatedAt(updatedAt)
                                .build()
                ))
                .pagination(GenerationRequestListResponse.PaginationInfo.builder()
                        .currentPage(1)
                        .pageSize(10)
                        .totalCount(1L)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build())
                .build();
        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(generationRequestService.getRequests(eq(user), any())).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/requests")
                        .param("page", "1")
                        .param("size", "10")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("data").size()).isEqualTo(1);
        assertThat(body.get("data").get(0).get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(body.get("data").get(0).get("concept").asText()).isEqualTo("봄 프로모션");
        assertThat(body.get("data").get(0).get("thumbnailUrl").asText()).isEqualTo("https://signed.example.com/thumb");
        assertThat(body.get("data").get(0).get("imageCount").asInt()).isEqualTo(2);
        assertThat(LocalDateTime.parse(body.get("data").get(0).get("createdAt").asText())).isEqualTo(createdAt);
        assertThat(LocalDateTime.parse(body.get("data").get(0).get("updatedAt").asText())).isEqualTo(updatedAt);
        assertThat(body.get("pagination").get("currentPage").asInt()).isEqualTo(1);
        assertThat(body.get("pagination").get("pageSize").asInt()).isEqualTo(10);
        assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(1L);
        assertThat(body.get("pagination").get("totalPages").asInt()).isEqualTo(1);
        assertThat(body.get("pagination").get("hasNext").asBoolean()).isFalse();
        assertThat(body.get("pagination").get("hasPrevious").asBoolean()).isFalse();
        then(generationRequestService).should().getRequests(
                eq(user),
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 10)
        );
    }

    @Test
    @DisplayName("생성 요청 상세 조회 성공")
    void shouldReturnGenerationRequestDetail() throws Exception {
        // 인증된 사용자가 요청 상세를 조회하면 이미지 포함 상세 정보를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 18, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 18, 40);
        LocalDateTime imageCreatedAt = LocalDateTime.of(2026, 3, 30, 18, 31);
        GenerationRequestDetailResponse response = GenerationRequestDetailResponse.builder()
                .id(requestId)
                .userId(user.getId())
                .concept("봄 프로모션")
                .additionalNote("따뜻한 분위기")
                .targetAge("20대")
                .targetGender("여성")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .images(List.of(
                        GenerationRequestDetailResponse.ImageResponse.builder()
                                .id(imageId)
                                .url("https://signed.example.com/image")
                                .description("정면 이미지")
                                .displayOrder(1)
                                .createdAt(imageCreatedAt)
                                .build()
                ))
                .build();
        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(generationRequestService.getRequestDetail(user, requestId)).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/request/{requestId}", requestId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("id").asText()).isEqualTo(requestId.toString());
        assertThat(body.get("userId").asText()).isEqualTo(user.getId().toString());
        assertThat(body.get("concept").asText()).isEqualTo("봄 프로모션");
        assertThat(body.get("images").size()).isEqualTo(1);
        assertThat(body.get("images").get(0).get("id").asText()).isEqualTo(imageId.toString());
        assertThat(body.get("images").get(0).get("url").asText()).isEqualTo("https://signed.example.com/image");
        assertThat(body.get("images").get(0).get("description").asText()).isEqualTo("정면 이미지");
        assertThat(body.get("images").get(0).get("displayOrder").asInt()).isEqualTo(1);
        assertThat(LocalDateTime.parse(body.get("createdAt").asText())).isEqualTo(createdAt);
        assertThat(LocalDateTime.parse(body.get("updatedAt").asText())).isEqualTo(updatedAt);
        assertThat(LocalDateTime.parse(body.get("images").get(0).get("createdAt").asText())).isEqualTo(imageCreatedAt);
    }

    @Test
    @DisplayName("생성 요청 수정 성공")
    void shouldUpdateGenerationRequest() throws Exception {
        // deletedImageIds 없이 imageConfigs만 전달해 수정 응답을 반환해야 한다.

        // given
        UUID requestId = UUID.randomUUID();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 18, 50);
        String imageConfigs = """
                [
                  {"id":"%s","description":"수정된 설명","displayOrder":1},
                  {"fileName":"new-image.png","description":"신규 이미지","displayOrder":2}
                ]
                """.formatted(UUID.randomUUID());
        MockMultipartFile newImage = createMultipartFile("newImages", "new-image.png", "new-image");
        UpdateGenerationResponse response = UpdateGenerationResponse.builder()
                .requestId(requestId)
                .concept("수정된 컨셉")
                .additionalNote("수정된 메모")
                .targetAge("20대")
                .targetGender("여성")
                .updatedAt(updatedAt)
                .imageSummary(UpdateGenerationResponse.ImageSummary.builder()
                        .totalCount(2)
                        .addedCount(1)
                        .deletedCount(1)
                        .build())
                .build();
        given(generationRequestService.updateRequest(eq("user@example.com"), eq(requestId), any())).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(multipart("/api/v1/contents/request/{requestId}", requestId)
                        .file(newImage)
                        .param("concept", "수정된 컨셉")
                        .param("additionalNote", "수정된 메모")
                        .param("targetAge", "20대")
                        .param("targetGender", "여성")
                        .param("imageConfigs", imageConfigs)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("requestId").asText()).isEqualTo(requestId.toString());
        assertThat(body.get("concept").asText()).isEqualTo("수정된 컨셉");
        assertThat(body.get("additionalNote").asText()).isEqualTo("수정된 메모");
        assertThat(body.get("imageSummary").get("totalCount").asInt()).isEqualTo(2);
        assertThat(body.get("imageSummary").get("addedCount").asInt()).isEqualTo(1);
        assertThat(body.get("imageSummary").get("deletedCount").asInt()).isEqualTo(1);
        assertThat(LocalDateTime.parse(body.get("updatedAt").asText())).isEqualTo(updatedAt);
        then(generationRequestService).should().updateRequest(
                eq("user@example.com"),
                eq(requestId),
                argThat(request ->
                        request.getConcept().equals("수정된 컨셉")
                                && request.getAdditionalNote().equals("수정된 메모")
                                && request.getTargetAge().equals("20대")
                                && request.getTargetGender().equals("여성")
                                && request.getImageConfigs().equals(imageConfigs)
                                && request.getNewImages().size() == 1
                )
        );
    }

    @Test
    @DisplayName("생성 요청 삭제 성공")
    void shouldDeleteGenerationRequest() throws Exception {
        // 인증된 사용자가 요청을 삭제하면 204 응답을 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        given(userService.getUser(any(Authentication.class))).willReturn(user);

        // when
        MvcResult result = mockMvc.perform(delete("/api/v1/contents/request/{requestId}", requestId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isBlank();
        then(userService).should().getUser(any(Authentication.class));
        then(generationRequestService).should().deleteRequest(user, requestId);
    }

    @Test
    @DisplayName("생성 요청 생성 검증 오류")
    void shouldReturnBadRequestWhenCreateRequestIsInvalid() throws Exception {
        // 필수 입력 없이 생성 요청하면 C001 검증 에러를 반환해야 한다.

        // given
        MockMultipartFile image = createMultipartFile("images", "first.png", "first-image");

        // when
        MvcResult result = mockMvc.perform(multipart("/api/v1/contents/request")
                        .file(image)
                        .param("imageDescriptions", "정면 이미지")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("C001");
        assertThat(body.get("errors").size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("생성 요청 수정 검증 오류")
    void shouldReturnBadRequestWhenUpdateRequestIsInvalid() throws Exception {
        // imageConfigs가 비어 있으면 C001 검증 에러를 반환해야 한다.

        // given
        UUID requestId = UUID.randomUUID();

        // when
        MvcResult result = mockMvc.perform(multipart("/api/v1/contents/request/{requestId}", requestId)
                        .param("concept", "수정된 컨셉")
                        .param("imageConfigs", "")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("C001");
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("imageConfigs");
    }

    @Test
    @DisplayName("생성 요청 목록 페이징 검증 오류")
    void shouldReturnBadRequestWhenPageRequestIsInvalid() throws Exception {
        // 잘못된 page, size 값이면 P001 검증 에러를 반환해야 한다.

        // given

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/requests")
                        .param("page", "0")
                        .param("size", "101")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("P001");
        assertThat(body.get("errors").size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("생성 요청 생성 요청 제한")
    void shouldReturnTooManyRequestsWhenCreateRequestRateLimitIsExceeded() throws Exception {
        // 생성 요청 엔드포인트는 MIDDLE 티어 레이트리밋을 적용해야 한다.

        // given
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(multipart("/api/v1/contents/request")
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        then(rateLimitService).should().tryConsume(CLIENT_IP, RateLimitTier.MIDDLE);
    }

    @Test
    @DisplayName("생성 요청 목록 조회 요청 제한")
    void shouldReturnTooManyRequestsWhenListRequestRateLimitIsExceeded() throws Exception {
        // 목록 조회 엔드포인트는 LOW 티어 레이트리밋을 적용해야 한다.

        // given
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.LOW)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/requests")
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        then(rateLimitService).should().tryConsume(CLIENT_IP, RateLimitTier.LOW);
    }

    @Test
    @DisplayName("생성 요청 상세 조회 요청 제한")
    void shouldReturnTooManyRequestsWhenDetailRequestRateLimitIsExceeded() throws Exception {
        // 상세 조회 엔드포인트는 LOW 티어 레이트리밋을 적용해야 한다.

        // given
        UUID requestId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.LOW)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/contents/request/{requestId}", requestId)
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        then(rateLimitService).should().tryConsume(CLIENT_IP, RateLimitTier.LOW);
    }

    @Test
    @DisplayName("생성 요청 수정 요청 제한")
    void shouldReturnTooManyRequestsWhenUpdateRequestRateLimitIsExceeded() throws Exception {
        // 수정 엔드포인트는 MIDDLE 티어 레이트리밋을 적용해야 한다.

        // given
        UUID requestId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(multipart("/api/v1/contents/request/{requestId}", requestId)
                        .header("X-Forwarded-For", CLIENT_IP)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        then(rateLimitService).should().tryConsume(CLIENT_IP, RateLimitTier.MIDDLE);
    }

    @Test
    @DisplayName("생성 요청 삭제 요청 제한")
    void shouldReturnTooManyRequestsWhenDeleteRequestRateLimitIsExceeded() throws Exception {
        // 삭제 엔드포인트는 MIDDLE 티어 레이트리밋을 적용해야 한다.

        // given
        UUID requestId = UUID.randomUUID();
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(delete("/api/v1/contents/request/{requestId}", requestId)
                        .header("X-Forwarded-For", CLIENT_IP))
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
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private User createUser(String email) {
        User user = new User(email, "테스트 사용자", "google", UUID.randomUUID().toString(), "https://image.test/profile.png");
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private MockMultipartFile createMultipartFile(String parameterName, String originalFilename, String content) {
        return new MockMultipartFile(parameterName, originalFilename, "image/png", content.getBytes());
    }
}
