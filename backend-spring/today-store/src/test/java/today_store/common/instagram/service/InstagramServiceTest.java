package today_store.common.instagram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import today_store.authentication.entity.User;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.common.instagram.client.InstagramClient;
import today_store.common.instagram.dto.InstagramAuthRequest;
import today_store.common.instagram.dto.InstagramAuthResponse;
import today_store.common.instagram.dto.InstagramPublishRequest;
import today_store.common.instagram.dto.InstagramPublishResponse;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentImage;
import today_store.content.content.entity.ContentPlatform;
import today_store.content.content.entity.ContentPost;
import today_store.content.content.repository.ContentImageRepository;
import today_store.content.content.repository.ContentRepository;
import today_store.content.publish.service.PublishPostService;
import today_store.content.request.entity.GenerationRequest;
import today_store.user.entity.UserSocialAccount;
import today_store.user.repository.UserSocialAccountRepository;
import today_store.user.service.UserSocialAccountService;

@ExtendWith(MockitoExtension.class)
@DisplayName("인스타그램 서비스 테스트")
class InstagramServiceTest {

    @Mock
    private InstagramClient instagramClient;

    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;

    @Mock
    private UserSocialAccountService userSocialAccountService;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentImageRepository contentImageRepository;

    @Mock
    private PublishPostService publishPostService;

    @Mock
    private GcsService gcsService;

    private InstagramService instagramService;

    @BeforeEach
    void setUp() {
        instagramService = new InstagramService(
                instagramClient,
                userSocialAccountRepository,
                userSocialAccountService,
                contentRepository,
                contentImageRepository,
                publishPostService,
                gcsService
        );
    }

    @Test
    @DisplayName("인스타그램 인증 시 장기 토큰 계정 연결")
    void shouldAuthenticateAndLinkInstagramAccount() {
        // 인스타그램 OAuth는 단기 인증 코드를 장기 토큰으로 교환한 뒤
        // 조회된 인스타그램 식별자와 함께 장기 토큰만 계정에 연결해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        InstagramAuthRequest request = new InstagramAuthRequest();
        request.setAuthCode("auth-code");
        request.setRedirectUri("https://app.test/oauth/instagram/callback");
        LocalDateTime beforeCall = LocalDateTime.now();

        given(instagramClient.getShortLivedToken("auth-code", "https://app.test/oauth/instagram/callback"))
                .willReturn("short-lived-token");
        given(instagramClient.getLongLivedToken("short-lived-token"))
                .willReturn(Map.of("access_token", "long-lived-token", "expires_in", 3600));
        given(instagramClient.getUserInfo("long-lived-token"))
                .willReturn(Map.of("id", "ig-user-id", "username", "today_store"));

        // when
        InstagramAuthResponse response = instagramService.authenticate(user, request);

        // then
        assertThat(response.getInstagramUserId()).isEqualTo("ig-user-id");
        assertThat(response.getUsername()).isEqualTo("today_store");
        then(userSocialAccountService).should().linkAccount(
                eq(user),
                eq("INSTAGRAM"),
                eq("ig-user-id"),
                eq("today_store"),
                eq("long-lived-token"),
                argThat(expiresAt ->
                        !expiresAt.isBefore(beforeCall.plusSeconds(3599))
                                && !expiresAt.isAfter(LocalDateTime.now().plusSeconds(3601))
                )
        );
    }

    @Test
    @DisplayName("단일 이미지 게시 성공 시 게시 완료 처리")
    void shouldPublishSingleImageAndCompletePost() {
        // 인스타그램 게시 성공 시 signed 이미지 URL과 해시태그가 포함된 캡션으로
        // 미디어를 게시하고, 인스타그램 메타데이터로 진행 중 게시를 완료해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        Content content = createContent(user);
        ContentImage image = createImage(content, "contents/instagram.png");
        UserSocialAccount account = createInstagramAccount(user, true, LocalDateTime.now().plusDays(1));
        ContentPost inProgressPost = ContentPost.builder()
                .id(UUID.randomUUID())
                .content(content)
                .platform(ContentPlatform.INSTAGRAM)
                .build();
        InstagramPublishRequest request = createPublishRequest(content.getId());
        LocalDateTime expectedPublishedAt = LocalDateTime.of(2026, 4, 20, 10, 15, 30);

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(userSocialAccountRepository.findByUserAndPlatform(user, "INSTAGRAM")).willReturn(Optional.of(account));
        given(contentImageRepository.findByContentId(content.getId())).willReturn(List.of(image));
        given(publishPostService.createInProgressPost(content, ContentPlatform.INSTAGRAM)).willReturn(inProgressPost);
        given(gcsService.generateSignedUrl("contents/instagram.png")).willReturn("https://signed.test/instagram.png");
        given(instagramClient.createMediaContainer(
                "ig-user-id",
                "long-lived-token",
                "https://signed.test/instagram.png",
                "Instagram caption\n\n#fresh #sale",
                false
        )).willReturn("creation-id");
        given(instagramClient.publishMedia("ig-user-id", "long-lived-token", "creation-id")).willReturn("media-id");
        given(instagramClient.getMediaInfo("media-id", "long-lived-token"))
                .willReturn(Map.of(
                        "permalink", "https://instagram.com/p/media-id",
                        "timestamp", "2026-04-20T10:15:30+0000"
                ));

        // when
        InstagramPublishResponse response = instagramService.publish(user, request);

        // then
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getPostId()).isEqualTo(inProgressPost.getId());
        assertThat(response.getMediaId()).isEqualTo("media-id");
        assertThat(response.getLink()).isEqualTo("https://instagram.com/p/media-id");
        assertThat(response.getPublishedAt()).isEqualTo(expectedPublishedAt);
        then(publishPostService).should().completePost(
                inProgressPost.getId(),
                "media-id",
                "https://instagram.com/p/media-id",
                expectedPublishedAt
        );
        then(publishPostService).should(never()).failPost(any(UUID.class));
    }

    @Test
    @DisplayName("다중 이미지 게시 시 캐러셀 사용")
    void shouldPublishCarouselWhenContentHasMultipleImages() {
        // 이미지가 여러 장이면 먼저 캐러셀 하위 컨테이너를 생성하고
        // 최종 캡션을 가진 캐러셀 컨테이너를 통해 게시해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        Content content = createContent(user);
        ContentImage firstImage = createImage(content, "contents/first.png");
        ContentImage secondImage = createImage(content, "contents/second.png");
        UserSocialAccount account = createInstagramAccount(user, true, LocalDateTime.now().plusDays(1));
        ContentPost inProgressPost = ContentPost.builder()
                .id(UUID.randomUUID())
                .content(content)
                .platform(ContentPlatform.INSTAGRAM)
                .build();
        InstagramPublishRequest request = createPublishRequest(content.getId());
        LocalDateTime expectedPublishedAt = LocalDateTime.of(2026, 4, 20, 11, 20, 30);

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(userSocialAccountRepository.findByUserAndPlatform(user, "INSTAGRAM")).willReturn(Optional.of(account));
        given(contentImageRepository.findByContentId(content.getId())).willReturn(List.of(firstImage, secondImage));
        given(publishPostService.createInProgressPost(content, ContentPlatform.INSTAGRAM)).willReturn(inProgressPost);
        given(gcsService.generateSignedUrl("contents/first.png")).willReturn("https://signed.test/first.png");
        given(gcsService.generateSignedUrl("contents/second.png")).willReturn("https://signed.test/second.png");
        given(instagramClient.createMediaContainer(
                "ig-user-id",
                "long-lived-token",
                "https://signed.test/first.png",
                null,
                true
        )).willReturn("child-container-1");
        given(instagramClient.createMediaContainer(
                "ig-user-id",
                "long-lived-token",
                "https://signed.test/second.png",
                null,
                true
        )).willReturn("child-container-2");
        given(instagramClient.createCarouselContainer(
                "ig-user-id",
                "long-lived-token",
                List.of("child-container-1", "child-container-2"),
                "Instagram caption\n\n#fresh #sale"
        )).willReturn("carousel-container-id");
        given(instagramClient.publishMedia("ig-user-id", "long-lived-token", "carousel-container-id")).willReturn("media-id");
        given(instagramClient.getMediaInfo("media-id", "long-lived-token"))
                .willReturn(Map.of(
                        "permalink", "https://instagram.com/p/carousel-id",
                        "timestamp", "2026-04-20T11:20:30+0000"
                ));

        // when
        InstagramPublishResponse response = instagramService.publish(user, request);

        // then
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getLink()).isEqualTo("https://instagram.com/p/carousel-id");
        assertThat(response.getPublishedAt()).isEqualTo(expectedPublishedAt);
        then(instagramClient).should().createCarouselContainer(
                "ig-user-id",
                "long-lived-token",
                List.of("child-container-1", "child-container-2"),
                "Instagram caption\n\n#fresh #sale"
        );
        then(publishPostService).should().completePost(
                inProgressPost.getId(),
                "media-id",
                "https://instagram.com/p/carousel-id",
                expectedPublishedAt
        );
        then(publishPostService).should(never()).failPost(any(UUID.class));
    }

    @Test
    @DisplayName("인스타그램 토큰 만료 시 게시 거부")
    void shouldRejectPublishWhenInstagramTokenIsExpired() {
        // 토큰 유효성은 게시 초안 생성이나 외부 API 호출보다 먼저 확인되어야 하며
        // 만료된 인스타그램 인증 정보는 게시 기록을 남기지 않고 즉시 실패해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        Content content = createContent(user);
        UserSocialAccount expiredAccount = createInstagramAccount(user, true, LocalDateTime.now().minusMinutes(1));
        InstagramPublishRequest request = createPublishRequest(content.getId());

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(userSocialAccountRepository.findByUserAndPlatform(user, "INSTAGRAM")).willReturn(Optional.of(expiredAccount));

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> instagramService.publish(user, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSTAGRAM_TOKEN_EXPIRED);
        then(contentImageRepository).should(never()).findByContentId(any(UUID.class));
        then(publishPostService).should(never()).createInProgressPost(any(Content.class), any(ContentPlatform.class));
        then(instagramClient).should(never()).createMediaContainer(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("인스타그램 API 실패 시 게시 실패 처리")
    void shouldMarkPostFailedWhenInstagramPublishFailsAfterPostCreation() {
        // IN_PROGRESS 게시가 생성된 뒤 인스타그램 API가 실패하면
        // 콘텐츠 상태가 멈춰 있지 않도록 해당 게시를 FAILED로 보상 처리해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        Content content = createContent(user);
        ContentImage image = createImage(content, "contents/instagram.png");
        UserSocialAccount account = createInstagramAccount(user, true, LocalDateTime.now().plusDays(1));
        ContentPost inProgressPost = ContentPost.builder()
                .id(UUID.randomUUID())
                .content(content)
                .platform(ContentPlatform.INSTAGRAM)
                .build();
        InstagramPublishRequest request = createPublishRequest(content.getId());
        CustomException apiException = new CustomException(ErrorCode.INSTAGRAM_API_ERROR, "container creation failed");

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(userSocialAccountRepository.findByUserAndPlatform(user, "INSTAGRAM")).willReturn(Optional.of(account));
        given(contentImageRepository.findByContentId(content.getId())).willReturn(List.of(image));
        given(publishPostService.createInProgressPost(content, ContentPlatform.INSTAGRAM)).willReturn(inProgressPost);
        given(gcsService.generateSignedUrl("contents/instagram.png")).willReturn("https://signed.test/instagram.png");
        given(instagramClient.createMediaContainer(
                "ig-user-id",
                "long-lived-token",
                "https://signed.test/instagram.png",
                "Instagram caption\n\n#fresh #sale",
                false
        )).willThrow(apiException);

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> instagramService.publish(user, request)
        );

        // then
        assertThat(exception).isSameAs(apiException);
        then(publishPostService).should().failPost(inProgressPost.getId());
        then(publishPostService).should(never()).completePost(any(UUID.class), any(), any(), any(LocalDateTime.class));
    }

    private User createUser(UUID id, String email) {
        User user = new User(email, "Test User", "google", id.toString(), "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content createContent(User user) {
        GenerationRequest generationRequest = GenerationRequest.builder()
                .id(UUID.randomUUID())
                .user(user)
                .concept("new menu")
                .build();
        return Content.builder()
                .id(UUID.randomUUID())
                .generationRequest(generationRequest)
                .instagramText("Instagram caption")
                .instagramHashtags(List.of("#fresh", "#sale"))
                .build();
    }

    private ContentImage createImage(Content content, String url) {
        return ContentImage.builder()
                .id(UUID.randomUUID())
                .content(content)
                .inputImageId(UUID.randomUUID())
                .url(url)
                .build();
    }

    private UserSocialAccount createInstagramAccount(User user, boolean valid, LocalDateTime expiresAt) {
        UserSocialAccount account = new UserSocialAccount();
        account.setUser(user);
        account.setPlatform("INSTAGRAM");
        account.setSocialUserId("ig-user-id");
        account.setUsername("today_store");
        account.setAccessToken("long-lived-token");
        account.setTokenExpiresAt(expiresAt);
        account.setIsValid(valid);
        return account;
    }

    private InstagramPublishRequest createPublishRequest(UUID contentId) {
        InstagramPublishRequest request = new InstagramPublishRequest();
        request.setContentId(contentId);
        return request;
    }
}
