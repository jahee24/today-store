package today_store.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import today_store.store.dto.CreateStoreResponse;
import today_store.store.dto.SnsInfo;
import today_store.store.dto.StoreResponse;
import today_store.store.dto.UpdateStoreResponse;
import today_store.store.entity.PreferredStyle;
import today_store.store.service.StoreService;
import today_store.user.service.UserService;

@WebMvcTest(controllers = StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ValidationErrorResolver.class)
@DisplayName("가게 컨트롤러 테스트")
class StoreControllerWebMvcTest {

    private static final String CLIENT_IP = "203.0.113.20";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        given(rateLimitService.tryConsume(anyString(), any(RateLimitTier.class))).willReturn(true);
    }

    @Test
    @DisplayName("가게 생성 성공")
    void shouldCreateStore() throws Exception {
        // 인증된 사용자가 가게를 생성하면 201 응답과 생성 정보를 반환해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        UUID storeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 15, 0);
        CreateStoreResponse response = CreateStoreResponse.builder()
                .id(storeId)
                .createdAt(createdAt)
                .build();
        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(storeService.createStore(eq(user), any())).willReturn(response);
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "storeName", "오늘카페",
                "businessType", "카페",
                "address", "서울시 성동구",
                "latitude", "37.12345678",
                "longitude", "127.87654321"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/stores")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(body.get("id").asText()).isEqualTo(storeId.toString());
        assertThat(LocalDateTime.parse(body.get("createdAt").asText())).isEqualTo(createdAt);
        then(userService).should().getUser(any(Authentication.class));
        then(storeService).should().createStore(
                eq(user),
                argThat(request ->
                        request.getStoreName().equals("오늘카페")
                                && request.getBusinessType().equals("카페")
                                && request.getAddress().equals("서울시 성동구")
                )
        );
    }

    @Test
    @DisplayName("내 가게 조회 성공")
    void shouldReturnMyStore() throws Exception {
        // 인증된 사용자가 자신의 가게를 조회하면 가게 프로필을 반환해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        UUID storeId = UUID.randomUUID();
        StoreResponse response = StoreResponse.builder()
                .id(storeId)
                .storeName("오늘카페")
                .businessType("카페")
                .address("서울시 성동구")
                .latitude(new java.math.BigDecimal("37.12345678"))
                .longitude(new java.math.BigDecimal("127.87654321"))
                .preferredStyle(PreferredStyle.FRIENDLY)
                .sns(SnsInfo.builder()
                        .instagram("@todaycafe")
                        .naver("https://naver.me/todaycafe")
                        .karrot("https://www.daangn.com/todaycafe")
                        .build())
                .build();
        given(userService.getUser(any(Authentication.class))).willReturn(user);
        given(storeService.getStore(user)).willReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/stores/me")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken()))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("id").asText()).isEqualTo(storeId.toString());
        assertThat(body.get("storeName").asText()).isEqualTo("오늘카페");
        assertThat(body.get("businessType").asText()).isEqualTo("카페");
        assertThat(body.get("preferredStyle").asText()).isEqualTo("FRIENDLY");
        assertThat(body.get("sns").get("instagram").asText()).isEqualTo("@todaycafe");
        assertThat(body.get("sns").get("naver").asText()).isEqualTo("https://naver.me/todaycafe");
        assertThat(body.get("sns").get("karrot").asText()).isEqualTo("https://www.daangn.com/todaycafe");
        then(userService).should().getUser(any(Authentication.class));
        then(storeService).should().getStore(user);
    }

    @Test
    @DisplayName("내 가게 수정 성공")
    void shouldUpdateMyStore() throws Exception {
        // 인증된 사용자가 가게 정보를 수정하면 수정 응답을 반환해야 한다.

        // given
        UUID storeId = UUID.randomUUID();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 15, 20);
        UpdateStoreResponse response = UpdateStoreResponse.builder()
                .id(storeId)
                .updatedAt(updatedAt)
                .build();
        given(storeService.updateStore(eq("user@example.com"), any())).willReturn(response);
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "storeName", "오늘카페 리뉴얼",
                "preferredStyle", "MEME",
                "snsInstagram", "@renewedcafe",
                "address", "서울시 송파구",
                "latitude", "37.00000000",
                "longitude", "128.00000000"
        ));

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/stores/me")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.get("id").asText()).isEqualTo(storeId.toString());
        assertThat(LocalDateTime.parse(body.get("updatedAt").asText())).isEqualTo(updatedAt);
        then(storeService).should().updateStore(
                eq("user@example.com"),
                argThat(request ->
                        request.getStoreName().equals("오늘카페 리뉴얼")
                                && request.getPreferredStyle() == PreferredStyle.MEME
                                && request.getSnsInstagram().equals("@renewedcafe")
                                && request.getAddress().equals("서울시 송파구")
                )
        );
    }

    @Test
    @DisplayName("가게 생성 검증 오류")
    void shouldReturnBadRequestWhenCreateStoreRequestIsInvalid() throws Exception {
        // 필수값 없이 가게 생성 요청하면 S001 검증 에러를 반환해야 한다.

        // given
        String requestBody = "{}";

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/stores")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .principal(authenticationToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body.get("code").asText()).isEqualTo("S001");
        assertThat(body.get("message").asText()).isEqualTo("Store name and business type are required");
        assertThat(body.get("errors").size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("내 가게 수정 인증 오류")
    void shouldReturnUnauthorizedWhenUpdatingStoreWithoutAuthentication() throws Exception {
        // 인증 정보 없이 가게 수정 요청하면 A005 인증 에러를 반환해야 한다.

        // given
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "storeName", "오늘카페 리뉴얼"
        ));

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/stores/me")
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
    @DisplayName("가게 생성 요청 제한")
    void shouldReturnTooManyRequestsWhenCreateStoreRateLimitIsExceeded() throws Exception {
        // 생성 요청 제한을 초과하면 R001 에러와 MIDDLE 티어 호출을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "storeName", "오늘카페",
                "businessType", "카페",
                "address", "서울시 성동구",
                "latitude", "37.12345678",
                "longitude", "127.87654321"
        ));

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/stores")
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

    @Test
    @DisplayName("내 가게 조회 요청 제한")
    void shouldReturnTooManyRequestsWhenGetStoreRateLimitIsExceeded() throws Exception {
        // 조회 요청 제한을 초과하면 R001 에러와 LOW 티어 호출을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.LOW)).willReturn(false);

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/stores/me")
                        .header("X-Forwarded-For", CLIENT_IP))
                .andReturn();
        JsonNode body = readBody(result);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("R001");
        then(rateLimitService).should().tryConsume(CLIENT_IP, RateLimitTier.LOW);
    }

    @Test
    @DisplayName("내 가게 수정 요청 제한")
    void shouldReturnTooManyRequestsWhenUpdateStoreRateLimitIsExceeded() throws Exception {
        // 수정 요청 제한을 초과하면 R001 에러와 MIDDLE 티어 호출을 반환해야 한다.

        // given
        given(rateLimitService.tryConsume(CLIENT_IP, RateLimitTier.MIDDLE)).willReturn(false);
        String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "storeName", "오늘카페 리뉴얼"
        ));

        // when
        MvcResult result = mockMvc.perform(patch("/api/v1/stores/me")
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
