package today_store.content.publish.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
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
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.common.gcs.GcsService;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentImage;
import today_store.content.content.entity.ContentPlatform;
import today_store.content.content.entity.ContentPost;
import today_store.content.content.entity.PublishStatus;
import today_store.content.content.exception.ContentNotFoundException;
import today_store.content.content.repository.ContentImageRepository;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.repository.ContentRepository;
import today_store.content.publish.dto.CompleteManualPublishRequest;
import today_store.content.publish.dto.CompleteManualPublishResponse;
import today_store.content.publish.dto.StartManualPublishRequest;
import today_store.content.publish.dto.StartManualPublishResponse;
import today_store.content.request.entity.GenerationRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("수동 게시 서비스 테스트")
class ManualPublishServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentPostRepository contentPostRepository;

    @Mock
    private ContentImageRepository contentImageRepository;

    @Mock
    private GcsService gcsService;

    private ManualPublishService manualPublishService;

    @BeforeEach
    void setUp() {
        manualPublishService = new ManualPublishService(
                contentRepository,
                contentPostRepository,
                contentImageRepository,
                gcsService
        );
    }

    @Test
    @DisplayName("수동 게시 시작 시 게시 초안과 플랫폼 데이터를 반환")
    void shouldStartManualPublishWithPlatformPayloadAndSignedImages() {
        // 수동 게시 시작 응답은 외부 업로드 화면에 전달되는 계약이므로
        // IN_PROGRESS 게시 초안을 생성하거나 재사용하고 플랫폼별 문구, 태그,
        // signed 이미지 URL, 이전 게시 정보를 함께 반환해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        Content content = createContent(user);
        StartManualPublishRequest request = new StartManualPublishRequest(content.getId(), ContentPlatform.INSTAGRAM);
        UUID postId = UUID.randomUUID();
        ContentPost inProgressPost = ContentPost.builder()
                .id(postId)
                .content(content)
                .platform(ContentPlatform.INSTAGRAM)
                .status(PublishStatus.IN_PROGRESS)
                .build();
        LocalDateTime lastPublishedAt = LocalDateTime.of(2026, 4, 20, 12, 30);
        ContentPost completedPost = ContentPost.builder()
                .id(UUID.randomUUID())
                .content(content)
                .platform(ContentPlatform.INSTAGRAM)
                .status(PublishStatus.COMPLETED)
                .publishedAt(lastPublishedAt)
                .build();
        ContentImage firstImage = createImage(content, "contents/first.png");
        ContentImage secondImage = createImage(content, "contents/second.png");

        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));
        given(contentPostRepository.findFirstByContentAndPlatformAndStatusOrderByPublishedAtDesc(
                content, ContentPlatform.INSTAGRAM, PublishStatus.IN_PROGRESS
        )).willReturn(Optional.empty());
        given(contentPostRepository.save(argThat(post ->
                post.getContent() == content
                        && post.getPlatform() == ContentPlatform.INSTAGRAM
                        && post.getStatus() == PublishStatus.IN_PROGRESS
        ))).willReturn(inProgressPost);
        given(contentPostRepository.findFirstByContentAndPlatformAndStatusOrderByPublishedAtDesc(
                content, ContentPlatform.INSTAGRAM, PublishStatus.COMPLETED
        )).willReturn(Optional.of(completedPost));
        given(contentImageRepository.findByContentOrderByCreatedAtAsc(content)).willReturn(List.of(firstImage, secondImage));
        given(gcsService.generateSignedUrl("contents/first.png")).willReturn("https://signed.test/first.png");
        given(gcsService.generateSignedUrl("contents/second.png")).willReturn("https://signed.test/second.png");

        // when
        StartManualPublishResponse response = manualPublishService.startManualPublish(user, request);

        // then
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getText()).isEqualTo("Instagram caption");
        assertThat(response.getTags()).containsExactly("#fresh", "#sale");
        assertThat(response.getImageUrls()).containsExactly("https://signed.test/first.png", "https://signed.test/second.png");
        assertThat(response.getStatus()).isEqualTo(PublishStatus.IN_PROGRESS);
        assertThat(response.isAlreadyPublished()).isTrue();
        assertThat(response.getLastPublishedAt()).isEqualTo(lastPublishedAt);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠의 수동 게시 시작 거부")
    void shouldThrowContentNotFoundWhenStartingManualPublishForMissingContent() {
        // 삭제되었거나 존재하지 않는 콘텐츠로 수동 게시 세션을 열면
        // 유효하지 않은 리소스에 대한 외부 업로드가 계속될 수 있으므로 거부해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        UUID missingContentId = UUID.randomUUID();
        StartManualPublishRequest request = new StartManualPublishRequest(missingContentId, ContentPlatform.NAVER);

        given(contentRepository.findByIdAndIsDeletedFalse(missingContentId)).willReturn(Optional.empty());

        // when
        ContentNotFoundException exception = assertThrows(
                ContentNotFoundException.class,
                () -> manualPublishService.startManualPublish(user, request)
        );

        // then
        assertThat(exception).isNotNull();
        then(contentPostRepository).should(never()).save(any(ContentPost.class));
        then(contentImageRepository).should(never()).findByContentOrderByCreatedAtAsc(any(Content.class));
    }

    @Test
    @DisplayName("수동 게시 시작 시 소유자 검증")
    void shouldThrowAccessDeniedWhenStartingManualPublishForAnotherUsersContent() {
        // 콘텐츠 소유권은 수동 게시의 주요 권한 경계이므로
        // 다른 사용자는 게시 ID, 문구, 태그, signed 이미지 URL을 받을 수 없어야 한다.

        // given
        User owner = createUser(UUID.randomUUID(), "owner@example.com");
        User requester = createUser(UUID.randomUUID(), "requester@example.com");
        Content content = createContent(owner);
        StartManualPublishRequest request = new StartManualPublishRequest(content.getId(), ContentPlatform.INSTAGRAM);

        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> manualPublishService.startManualPublish(requester, request)
        );

        // then
        assertThat(exception).isNotNull();
        then(contentPostRepository).should(never()).save(any(ContentPost.class));
        then(gcsService).should(never()).generateSignedUrl(any());
    }

    @Test
    @DisplayName("수동 게시 완료 시 게시 완료 상태로 저장")
    void shouldCompleteManualPublishWithPostUrl() {
        // 수동 게시 완료는 외부 게시 URL과 게시 시간을 기록하여
        // 콘텐츠 목록에서 게시 성공 상태를 확인할 수 있게 해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        Content content = createContent(user);
        ContentPost contentPost = ContentPost.builder()
                .id(UUID.randomUUID())
                .content(content)
                .platform(ContentPlatform.NAVER)
                .status(PublishStatus.IN_PROGRESS)
                .build();
        CompleteManualPublishRequest request = new CompleteManualPublishRequest(
                contentPost.getId(),
                PublishStatus.COMPLETED,
                "https://blog.naver.com/today-store/post"
        );

        given(contentPostRepository.findById(contentPost.getId())).willReturn(Optional.of(contentPost));
        given(contentPostRepository.save(contentPost)).willReturn(contentPost);

        // when
        CompleteManualPublishResponse response = manualPublishService.completeManualPublish(user, request);

        // then
        assertThat(response.getPostId()).isEqualTo(contentPost.getId());
        assertThat(response.getStatus()).isEqualTo(PublishStatus.COMPLETED);
        assertThat(response.getPublishedAt()).isNotNull();
        assertThat(contentPost.getPostUrl()).isEqualTo("https://blog.naver.com/today-store/post");
        then(contentPostRepository).should().save(contentPost);
    }

    @Test
    @DisplayName("수동 게시 실패 시 실패 상태로 저장")
    void shouldFailManualPublishAndClearPublicationFields() {
        // 외부 수동 업로드가 실패하면 게시 상태를 FAILED로 저장하고
        // 오래된 URL이나 게시 시간이 남지 않도록 정리해야 한다.

        // given
        User user = createUser(UUID.randomUUID(), "owner@example.com");
        Content content = createContent(user);
        ContentPost contentPost = ContentPost.builder()
                .id(UUID.randomUUID())
                .content(content)
                .platform(ContentPlatform.KARROT)
                .status(PublishStatus.IN_PROGRESS)
                .postUrl("https://stale.example/post")
                .publishedAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();
        CompleteManualPublishRequest request = new CompleteManualPublishRequest(
                contentPost.getId(),
                PublishStatus.FAILED,
                "https://ignored.example/post"
        );

        given(contentPostRepository.findById(contentPost.getId())).willReturn(Optional.of(contentPost));
        given(contentPostRepository.save(contentPost)).willReturn(contentPost);

        // when
        CompleteManualPublishResponse response = manualPublishService.completeManualPublish(user, request);

        // then
        assertThat(response.getPostId()).isEqualTo(contentPost.getId());
        assertThat(response.getStatus()).isEqualTo(PublishStatus.FAILED);
        assertThat(response.getPublishedAt()).isNull();
        assertThat(contentPost.getPostUrl()).isNull();
        assertThat(contentPost.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("수동 게시 완료 시 소유자 검증")
    void shouldThrowAccessDeniedWhenCompletingAnotherUsersPost() {
        // 완료 요청은 게시 상태를 변경하므로 수동 게시 시작 시점뿐 아니라
        // postId 기준으로도 다시 소유권을 검증해야 한다.

        // given
        User owner = createUser(UUID.randomUUID(), "owner@example.com");
        User requester = createUser(UUID.randomUUID(), "requester@example.com");
        Content content = createContent(owner);
        ContentPost contentPost = ContentPost.builder()
                .id(UUID.randomUUID())
                .content(content)
                .platform(ContentPlatform.INSTAGRAM)
                .status(PublishStatus.IN_PROGRESS)
                .build();
        CompleteManualPublishRequest request = new CompleteManualPublishRequest(
                contentPost.getId(),
                PublishStatus.COMPLETED,
                "https://instagram.com/p/test"
        );

        given(contentPostRepository.findById(contentPost.getId())).willReturn(Optional.of(contentPost));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> manualPublishService.completeManualPublish(requester, request)
        );

        // then
        assertThat(exception).isNotNull();
        then(contentPostRepository).should(never()).save(any(ContentPost.class));
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
                .naverText("Naver content")
                .naverKeywords(List.of("fresh", "sale"))
                .karrotText("Karrot content")
                .karrotTags(List.of("nearby", "deal"))
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
}
