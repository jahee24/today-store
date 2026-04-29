package today_store.content.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import today_store.authentication.entity.User;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.authentication.exception.UserNotFoundException;
import today_store.authentication.repository.UserRepository;
import today_store.common.config.GeminiConfig;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.common.gemini.dto.GeminiParsedResponse;
import today_store.common.gemini.dto.GeminiPromptRequest;
import today_store.common.gemini.dto.GeminiRegenerationRequest;
import today_store.common.gemini.service.GeminiService;
import today_store.content.content.dto.ContentListResponse;
import today_store.content.content.dto.ContentResponse;
import today_store.content.content.dto.GenerateContentResponse;
import today_store.content.content.dto.RegenerateContentRequest;
import today_store.content.content.dto.TaskStatusResponse;
import today_store.content.content.dto.UpdateContentRequest;
import today_store.content.content.entity.ApiLog;
import today_store.content.content.entity.ApiStatus;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentImage;
import today_store.content.content.entity.GenerationType;
import today_store.content.content.entity.PublishStatus;
import today_store.content.content.exception.ContentNotFoundException;
import today_store.content.content.exception.InvalidRegenerationRequestException;
import today_store.content.content.exception.TaskNotFoundException;
import today_store.content.content.repository.ApiLogRepository;
import today_store.content.content.repository.ContentImageRepository;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.repository.ContentRepository;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;
import today_store.content.request.exception.GenerationRequestNotFoundException;
import today_store.content.request.repository.GenerationRequestRepository;
import today_store.content.request.repository.InputImageRepository;
import today_store.store.entity.PreferredStyle;
import today_store.store.entity.Store;
import today_store.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("콘텐츠 서비스 테스트")
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentImageRepository contentImageRepository;

    @Mock
    private ApiLogRepository apiLogRepository;

    @Mock
    private GenerationRequestRepository requestRepository;

    @Mock
    private InputImageRepository inputImageRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentPostRepository contentPostRepository;

    @Mock
    private GeminiService geminiService;

    @Mock
    private GcsService gcsService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private GeminiConfig geminiConfig;

    private ContentService contentService;
    private TestableContentService testableContentService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<Object> callback = invocation.getArgument(0);
            TransactionStatus status = org.mockito.Mockito.mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        });

        contentService = new ContentService(
                contentRepository,
                contentImageRepository,
                apiLogRepository,
                requestRepository,
                inputImageRepository,
                storeRepository,
                userRepository,
                contentPostRepository,
                geminiService,
                gcsService,
                transactionTemplate,
                geminiConfig
        );

        testableContentService = new TestableContentService(
                contentRepository,
                contentImageRepository,
                apiLogRepository,
                requestRepository,
                inputImageRepository,
                storeRepository,
                userRepository,
                contentPostRepository,
                geminiService,
                gcsService,
                transactionTemplate,
                geminiConfig
        );
    }

    @Test
    @DisplayName("콘텐츠 생성 시작 성공")
    void shouldStartGeneration() {
        // 본인 요청으로 콘텐츠 생성을 시작하면 processing ApiLog를 저장하고 비동기 작업을 시작해야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        UUID apiLogId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        ApiLog savedLog = createApiLog(request, apiLogId, "gemini-2.5-flash", ApiStatus.PROCESSING);

        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(request));
        given(geminiConfig.getModel()).willReturn("gemini-2.5-flash");
        given(apiLogRepository.save(any(ApiLog.class))).willReturn(savedLog);

        // when
        GenerateContentResponse response = testableContentService.startGeneration(user, requestId);

        // then
        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getTaskId()).isEqualTo(apiLogId);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(testableContentService.generateAsyncCalled).isTrue();
        assertThat(testableContentService.capturedRequestId).isEqualTo(requestId);
        assertThat(testableContentService.capturedApiLogId).isEqualTo(apiLogId);
        then(apiLogRepository).should().save(argThat(log ->
                log.getGenerationRequest() == request
                        && log.getModel().equals("gemini-2.5-flash")
                        && log.getStatus() == ApiStatus.PROCESSING
        ));
    }

    @Test
    @DisplayName("존재하지 않는 요청 생성 시작 실패")
    void shouldThrowWhenStartingGenerationForMissingRequest() {
        // 생성 요청이 존재하지 않으면 ApiLog 저장 없이 예외를 반환해야 한다.

        // given
        given(requestRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

        // when
        GenerationRequestNotFoundException exception = assertThrows(
                GenerationRequestNotFoundException.class,
                () -> testableContentService.startGeneration(createUser("owner@example.com"), UUID.randomUUID())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND);
        then(apiLogRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("타 사용자 요청 생성 시작 거부")
    void shouldThrowWhenStartingGenerationForAnotherUsersRequest() {
        // 다른 사용자의 생성 요청으로 시작을 시도하면 접근 거부 예외를 반환해야 한다.

        // given
        User owner = createUser("owner@example.com");
        User viewer = createUser("viewer@example.com");
        GenerationRequest request = createGenerationRequest(owner, UUID.randomUUID());
        given(requestRepository.findByIdAndIsDeletedFalse(request.getId())).willReturn(Optional.of(request));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> testableContentService.startGeneration(viewer, request.getId())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
        then(apiLogRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("콘텐츠 생성 비동기 성공")
    void shouldGenerateContentAsync() throws Exception {
        // 비동기 생성이 성공하면 Content, ContentImage, 성공 ApiLog를 모두 저장해야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        UUID apiLogId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        Store store = createStore(user);
        InputImage firstInput = createInputImage(request, UUID.randomUUID(), "stored/input-1.png", "front", 1);
        InputImage secondInput = createInputImage(request, UUID.randomUUID(), "stored/input-2.png", "side", 2);
        ApiLog apiLog = createApiLog(request, apiLogId, "gemini-2.5-flash", ApiStatus.PROCESSING);
        GeminiParsedResponse parsed = GeminiParsedResponse.builder()
                .photoInfo("Bright cafe")
                .text("Fresh drinks for spring")
                .hashtags(List.of("#spring", "#cafe"))
                .inputTokens(1_000)
                .outputTokens(500)
                .responseTimeMs(321L)
                .build();
        CountDownLatch latch = new CountDownLatch(1);

        given(requestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(inputImageRepository.findByGenerationRequestOrderByDisplayOrderAsc(request)).willReturn(List.of(firstInput, secondInput));
        given(storeRepository.findByUser(user)).willReturn(Optional.of(store));
        given(gcsService.generateSignedUrl("stored/input-1.png")).willReturn("https://signed.example.com/input-1");
        given(gcsService.generateSignedUrl("stored/input-2.png")).willReturn("https://signed.example.com/input-2");
        given(geminiService.generateProcessedContent(any(GeminiPromptRequest.class))).willReturn(Mono.just(parsed));
        given(contentRepository.save(any(Content.class))).willAnswer(invocation -> {
            Content saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", contentId);
            return saved;
        });
        given(gcsService.copyFile("stored/input-1.png", "contents")).willReturn("stored/content-1.png");
        given(gcsService.copyFile("stored/input-2.png", "contents")).willReturn("stored/content-2.png");
        given(contentImageRepository.save(any(ContentImage.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(apiLogRepository.findById(apiLogId)).willReturn(Optional.of(apiLog));
        given(apiLogRepository.save(any(ApiLog.class))).willAnswer(invocation -> {
            ApiLog saved = invocation.getArgument(0);
            if (saved.getStatus() == ApiStatus.SUCCESS) {
                latch.countDown();
            }
            return saved;
        });

        // when
        contentService.generateContentAsync(requestId, apiLogId);

        awaitLatch(latch);

        // then
        ArgumentCaptor<GeminiPromptRequest> promptCaptor = ArgumentCaptor.forClass(GeminiPromptRequest.class);
        then(geminiService).should().generateProcessedContent(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getStoreName()).isEqualTo("Today Cafe");
        assertThat(promptCaptor.getValue().getBusinessType()).isEqualTo("Cafe");
        assertThat(promptCaptor.getValue().getAddress()).isEqualTo("Seoul");
        assertThat(promptCaptor.getValue().getTargetAge()).isEqualTo("20s");
        assertThat(promptCaptor.getValue().getTargetGender()).isEqualTo("ALL");
        assertThat(promptCaptor.getValue().getConcept()).isEqualTo("Spring promotion");
        assertThat(promptCaptor.getValue().getAdditionalNote()).isEqualTo("Bright mood");
        assertThat(promptCaptor.getValue().getImageDescriptions()).containsExactly("front", "side");
        assertThat(promptCaptor.getValue().getSignedUrls())
                .containsExactly("https://signed.example.com/input-1", "https://signed.example.com/input-2");

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        then(contentRepository).should().save(contentCaptor.capture());
        assertThat(contentCaptor.getValue().getGenerationRequest()).isEqualTo(request);
        assertThat(contentCaptor.getValue().getGenerationType()).isEqualTo(GenerationType.ALL);
        assertThat(contentCaptor.getValue().getInstagramText()).isEqualTo("Fresh drinks for spring");
        assertThat(contentCaptor.getValue().getKarrotText()).isEqualTo("Fresh drinks for spring");
        assertThat(contentCaptor.getValue().getNaverText()).isEqualTo("Fresh drinks for spring");

        ArgumentCaptor<ContentImage> imageCaptor = ArgumentCaptor.forClass(ContentImage.class);
        then(contentImageRepository).should(org.mockito.Mockito.times(2)).save(imageCaptor.capture());
        assertThat(imageCaptor.getAllValues()).hasSize(2);
        assertThat(imageCaptor.getAllValues().get(0).getInputImageId()).isEqualTo(firstInput.getId());
        assertThat(imageCaptor.getAllValues().get(0).getUrl()).isEqualTo("stored/content-1.png");
        assertThat(imageCaptor.getAllValues().get(1).getInputImageId()).isEqualTo(secondInput.getId());
        assertThat(imageCaptor.getAllValues().get(1).getUrl()).isEqualTo("stored/content-2.png");

        then(gcsService).should().copyFile("stored/input-1.png", "contents");
        then(gcsService).should().copyFile("stored/input-2.png", "contents");
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(apiLog.getContent()).isNotNull();
        assertThat(apiLog.getContent().getId()).isEqualTo(contentId);
        assertThat(apiLog.getInputTokens()).isEqualTo(1_000);
        assertThat(apiLog.getOutputTokens()).isEqualTo(500);
        assertThat(apiLog.getCostUsd()).isEqualByComparingTo("0.0020000000");
        assertThat(apiLog.getResponseTimeMs()).isEqualTo(321);
    }

    @Test
    @DisplayName("콘텐츠 생성 비동기 커스텀 예외 유지")
    void shouldStoreCustomExceptionMessageWhenGenerateContentAsyncFails() throws Exception {
        // 비동기 생성 중 CustomException이 발생하면 기존 에러 메시지를 그대로 ApiLog에 저장해야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        UUID apiLogId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        Store store = createStore(user);
        InputImage input = createInputImage(request, UUID.randomUUID(), "stored/input-1.png", "front", 1);
        ApiLog apiLog = createApiLog(request, apiLogId, "gemini-2.5-flash", ApiStatus.PROCESSING);
        CountDownLatch latch = new CountDownLatch(1);

        given(requestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(inputImageRepository.findByGenerationRequestOrderByDisplayOrderAsc(request)).willReturn(List.of(input));
        given(storeRepository.findByUser(user)).willReturn(Optional.of(store));
        given(gcsService.generateSignedUrl("stored/input-1.png")).willReturn("https://signed.example.com/input-1");
        given(geminiService.generateProcessedContent(any(GeminiPromptRequest.class)))
                .willReturn(Mono.error(new CustomException(ErrorCode.AI_PARSE_ERROR)));
        given(apiLogRepository.findById(apiLogId)).willReturn(Optional.of(apiLog));
        given(apiLogRepository.save(any(ApiLog.class))).willAnswer(invocation -> {
            ApiLog saved = invocation.getArgument(0);
            if (saved.getStatus() == ApiStatus.ERROR) {
                latch.countDown();
            }
            return saved;
        });

        // when
        contentService.generateContentAsync(requestId, apiLogId);

        awaitLatch(latch);

        // then
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(apiLog.getErrorMessage()).isEqualTo(ErrorCode.AI_PARSE_ERROR.getMessage());
        then(contentRepository).should(never()).save(any(Content.class));
    }

    @Test
    @DisplayName("콘텐츠 생성 비동기 예외 치환")
    void shouldStoreSanitizedMessageWhenGenerateContentAsyncFailsUnexpectedly() throws Exception {
        // 비동기 생성 중 예상치 못한 예외가 나면 안전한 AI003 메시지로 치환해 저장해야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        UUID apiLogId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        Store store = createStore(user);
        InputImage input = createInputImage(request, UUID.randomUUID(), "stored/input-1.png", "front", 1);
        ApiLog apiLog = createApiLog(request, apiLogId, "gemini-2.5-flash", ApiStatus.PROCESSING);
        CountDownLatch latch = new CountDownLatch(1);

        given(requestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(inputImageRepository.findByGenerationRequestOrderByDisplayOrderAsc(request)).willReturn(List.of(input));
        given(storeRepository.findByUser(user)).willReturn(Optional.of(store));
        given(gcsService.generateSignedUrl("stored/input-1.png")).willReturn("https://signed.example.com/input-1");
        given(geminiService.generateProcessedContent(any(GeminiPromptRequest.class)))
                .willReturn(Mono.error(new RuntimeException("secret key leaked")));
        given(apiLogRepository.findById(apiLogId)).willReturn(Optional.of(apiLog));
        given(apiLogRepository.save(any(ApiLog.class))).willAnswer(invocation -> {
            ApiLog saved = invocation.getArgument(0);
            if (saved.getStatus() == ApiStatus.ERROR) {
                latch.countDown();
            }
            return saved;
        });

        // when
        contentService.generateContentAsync(requestId, apiLogId);

        awaitLatch(latch);

        // then
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(apiLog.getErrorMessage()).isEqualTo(ErrorCode.AI_REQUEST_FAILED.getMessage());
        assertThat(apiLog.getErrorMessage()).doesNotContain("secret");
        then(contentRepository).should(never()).save(any(Content.class));
    }

    @Test
    @DisplayName("작업 상태 조회 성공")
    void shouldReturnTaskStatus() {
        // 작업 상태 조회 시 content가 있으면 result에 content id를 포함해야 한다.

        // given
        User user = createUser("owner@example.com");
        GenerationRequest request = createGenerationRequest(user, UUID.randomUUID());
        Content content = createContent(request, UUID.randomUUID(), GenerationType.ALL);
        ApiLog apiLog = createApiLog(request, UUID.randomUUID(), "gemini-2.5-flash", ApiStatus.SUCCESS);
        ReflectionTestUtils.setField(apiLog, "content", content);

        given(apiLogRepository.findById(apiLog.getId())).willReturn(Optional.of(apiLog));

        // when
        TaskStatusResponse response = contentService.getTaskStatus(user, apiLog.getId());

        // then
        assertThat(response.getTaskId()).isEqualTo(apiLog.getId());
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getResult()).isEqualTo(content.getId().toString());
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 작업 상태 조회 실패")
    void shouldThrowWhenTaskStatusIsMissing() {
        // 작업 ID가 존재하지 않으면 G002 예외를 반환해야 한다.

        // given
        given(apiLogRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        // when
        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> contentService.getTaskStatus(createUser("owner@example.com"), UUID.randomUUID())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @Test
    @DisplayName("타 사용자 작업 상태 조회 거부")
    void shouldThrowWhenGettingAnotherUsersTaskStatus() {
        // 다른 사용자의 작업 상태를 조회하면 접근 거부 예외를 반환해야 한다.

        // given
        User owner = createUser("owner@example.com");
        User viewer = createUser("viewer@example.com");
        GenerationRequest request = createGenerationRequest(owner, UUID.randomUUID());
        ApiLog apiLog = createApiLog(request, UUID.randomUUID(), "gemini-2.5-flash", ApiStatus.PROCESSING);
        given(apiLogRepository.findById(apiLog.getId())).willReturn(Optional.of(apiLog));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> contentService.getTaskStatus(viewer, apiLog.getId())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
    }

    @Test
    @DisplayName("콘텐츠 상세 조회 성공")
    void shouldReturnContentDetail() {
        // 콘텐츠 상세 조회 시 signed URL과 게시 여부를 함께 반환해야 한다.

        // given
        User user = createUser("owner@example.com");
        GenerationRequest request = createGenerationRequest(user, UUID.randomUUID());
        Content content = createContent(request, UUID.randomUUID(), GenerationType.ALL);
        ContentImage firstImage = createContentImage(content, UUID.randomUUID(), UUID.randomUUID(), "stored/content-1.png");
        ContentImage secondImage = createContentImage(content, UUID.randomUUID(), UUID.randomUUID(), "stored/content-2.png");

        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));
        given(contentImageRepository.findByContentOrderByCreatedAtAsc(content)).willReturn(List.of(firstImage, secondImage));
        given(contentPostRepository.existsByContentAndStatus(content, PublishStatus.COMPLETED)).willReturn(true);
        given(gcsService.generateSignedUrl("stored/content-1.png")).willReturn("https://signed.example.com/content-1");
        given(gcsService.generateSignedUrl("stored/content-2.png")).willReturn("https://signed.example.com/content-2");

        // when
        ContentResponse response = contentService.getContent(user, content.getId());

        // then
        assertThat(response.getId()).isEqualTo(content.getId());
        assertThat(response.getRequestId()).isEqualTo(request.getId());
        assertThat(response.getGenerationType()).isEqualTo(GenerationType.ALL);
        assertThat(response.getIsPosted()).isTrue();
        assertThat(response.getContentData().getInstagram().getText()).isEqualTo("Instagram body");
        assertThat(response.getContentData().getKarrot().getText()).isEqualTo("Karrot body");
        assertThat(response.getContentData().getNaver().getText()).isEqualTo("Naver body");
        assertThat(response.getImages()).hasSize(2);
        assertThat(response.getImages().get(0).getUrl()).isEqualTo("https://signed.example.com/content-1");
        assertThat(response.getImages().get(1).getUrl()).isEqualTo("https://signed.example.com/content-2");
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 상세 조회 실패")
    void shouldThrowWhenContentIsMissing() {
        // 콘텐츠가 존재하지 않으면 C007 예외를 반환해야 한다.

        // given
        given(contentRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

        // when
        ContentNotFoundException exception = assertThrows(
                ContentNotFoundException.class,
                () -> contentService.getContent(createUser("owner@example.com"), UUID.randomUUID())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
    }

    @Test
    @DisplayName("타 사용자 콘텐츠 상세 조회 거부")
    void shouldThrowWhenGettingAnotherUsersContent() {
        // 다른 사용자의 콘텐츠를 조회하면 접근 거부 예외를 반환해야 한다.

        // given
        User owner = createUser("owner@example.com");
        User viewer = createUser("viewer@example.com");
        Content content = createContent(createGenerationRequest(owner, UUID.randomUUID()), UUID.randomUUID(), GenerationType.ALL);
        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> contentService.getContent(viewer, content.getId())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
    }

    @Test
    @DisplayName("콘텐츠 목록 조회 성공")
    void shouldReturnContentList() {
        // 콘텐츠 목록 조회 시 게시 여부와 잘린 preview를 함께 반환해야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        Content firstContent = createContent(request, UUID.randomUUID(), GenerationType.ALL);
        Content secondContent = createContent(request, UUID.randomUUID(), GenerationType.TEXT_ONLY);
        ReflectionTestUtils.setField(firstContent, "instagramText", "123456789012345678901234567890XYZ");
        ReflectionTestUtils.setField(firstContent, "karrotText", "Short karrot");
        ReflectionTestUtils.setField(firstContent, "naverText", "");
        ReflectionTestUtils.setField(secondContent, "instagramText", null);
        ReflectionTestUtils.setField(secondContent, "karrotText", "Another short line");
        ReflectionTestUtils.setField(secondContent, "naverText", "Second naver preview");

        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(request));
        given(contentRepository.findByGenerationRequestAndIsDeletedFalseOrderByCreatedAtDesc(request))
                .willReturn(List.of(firstContent, secondContent));
        given(contentPostRepository.findContentIdsByContentInAndStatus(List.of(firstContent, secondContent), PublishStatus.COMPLETED))
                .willReturn(List.of(firstContent.getId()));

        // when
        ContentListResponse response = contentService.getContentList(user, requestId);

        // then
        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getContents()).hasSize(2);
        assertThat(response.getContents().get(0).getContentId()).isEqualTo(firstContent.getId());
        assertThat(response.getContents().get(0).getIsPosted()).isTrue();
        assertThat(response.getContents().get(0).getInstagramPreview()).isEqualTo("123456789012345678901234567890...");
        assertThat(response.getContents().get(0).getKarrotPreview()).isEqualTo("Short karrot");
        assertThat(response.getContents().get(1).getIsPosted()).isFalse();
        assertThat(response.getContents().get(1).getInstagramPreview()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 요청 콘텐츠 목록 조회 실패")
    void shouldThrowWhenContentListRequestIsMissing() {
        // 생성 요청이 존재하지 않으면 콘텐츠 목록 조회에 실패해야 한다.

        // given
        given(requestRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

        // when
        GenerationRequestNotFoundException exception = assertThrows(
                GenerationRequestNotFoundException.class,
                () -> contentService.getContentList(createUser("owner@example.com"), UUID.randomUUID())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("타 사용자 콘텐츠 목록 조회 거부")
    void shouldThrowWhenGettingAnotherUsersContentList() {
        // 다른 사용자의 생성 요청으로 목록 조회를 시도하면 접근 거부 예외를 반환해야 한다.

        // given
        User owner = createUser("owner@example.com");
        User viewer = createUser("viewer@example.com");
        GenerationRequest request = createGenerationRequest(owner, UUID.randomUUID());
        given(requestRepository.findByIdAndIsDeletedFalse(request.getId())).willReturn(Optional.of(request));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> contentService.getContentList(viewer, request.getId())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
    }

    @Test
    @DisplayName("콘텐츠 수정 성공")
    void shouldUpdateContentPartially() {
        // 콘텐츠 수정 시 요청에 포함된 플랫폼만 바꾸고 나머지는 유지해야 한다.

        // given
        User user = createUser("owner@example.com");
        Content content = createContent(createGenerationRequest(user, UUID.randomUUID()), UUID.randomUUID(), GenerationType.ALL);
        ContentImage image = createContentImage(content, UUID.randomUUID(), UUID.randomUUID(), "stored/content-1.png");
        UpdateContentRequest request = UpdateContentRequest.builder()
                .instagram(UpdateContentRequest.PlatformUpdate.builder()
                        .text("Updated instagram")
                        .hashtags(List.of("#new"))
                        .build())
                .naver(UpdateContentRequest.PlatformUpdate.builder()
                        .text("Updated naver")
                        .hashtags(null)
                        .build())
                .build();

        given(userRepository.findByEmail("owner@example.com")).willReturn(Optional.of(user));
        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));
        given(contentRepository.saveAndFlush(content)).willReturn(content);
        given(contentImageRepository.findByContentOrderByCreatedAtAsc(content)).willReturn(List.of(image));
        given(contentPostRepository.existsByContentAndStatus(content, PublishStatus.COMPLETED)).willReturn(false);
        given(gcsService.generateSignedUrl("stored/content-1.png")).willReturn("https://signed.example.com/content-1");

        // when
        ContentResponse response = contentService.updateContent("owner@example.com", content.getId(), request);

        // then
        assertThat(content.getInstagramText()).isEqualTo("Updated instagram");
        assertThat(content.getInstagramHashtags()).containsExactly("#new");
        assertThat(content.getKarrotText()).isEqualTo("Karrot body");
        assertThat(content.getKarrotTags()).containsExactly("#karrot");
        assertThat(content.getNaverText()).isEqualTo("Updated naver");
        assertThat(content.getNaverKeywords()).containsExactly("#naver");
        assertThat(response.getImages().get(0).getUrl()).isEqualTo("https://signed.example.com/content-1");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 콘텐츠 수정 실패")
    void shouldThrowWhenUpdatingContentForMissingUser() {
        // 사용자 이메일이 존재하지 않으면 콘텐츠 수정에 실패해야 한다.

        // given
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> contentService.updateContent("missing@example.com", UUID.randomUUID(), UpdateContentRequest.builder().build())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 수정 실패")
    void shouldThrowWhenUpdatingMissingContent() {
        // 콘텐츠가 존재하지 않으면 수정에 실패해야 한다.

        // given
        User user = createUser("owner@example.com");
        given(userRepository.findByEmail("owner@example.com")).willReturn(Optional.of(user));
        given(contentRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

        // when
        ContentNotFoundException exception = assertThrows(
                ContentNotFoundException.class,
                () -> contentService.updateContent("owner@example.com", UUID.randomUUID(), UpdateContentRequest.builder().build())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
    }

    @Test
    @DisplayName("타 사용자 콘텐츠 수정 거부")
    void shouldThrowWhenUpdatingAnotherUsersContent() {
        // 다른 사용자의 콘텐츠를 수정하면 접근 거부 예외를 반환해야 한다.

        // given
        User owner = createUser("owner@example.com");
        User viewer = createUser("viewer@example.com");
        Content content = createContent(createGenerationRequest(owner, UUID.randomUUID()), UUID.randomUUID(), GenerationType.ALL);
        given(userRepository.findByEmail("viewer@example.com")).willReturn(Optional.of(viewer));
        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> contentService.updateContent("viewer@example.com", content.getId(), UpdateContentRequest.builder().build())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
    }

    @Test
    @DisplayName("콘텐츠 삭제 성공")
    void shouldDeleteContent() {
        // 콘텐츠 삭제 시 soft delete 처리 후 저장해야 한다.

        // given
        User user = createUser("owner@example.com");
        Content content = createContent(createGenerationRequest(user, UUID.randomUUID()), UUID.randomUUID(), GenerationType.ALL);

        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));
        given(contentRepository.save(content)).willReturn(content);

        // when
        contentService.deleteContent(user, content.getId());

        // then
        assertThat(content.getIsDeleted()).isTrue();
        assertThat(content.getDeletedAt()).isNotNull();
        then(contentRepository).should().save(content);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 삭제 실패")
    void shouldThrowWhenDeletingMissingContent() {
        // 콘텐츠가 존재하지 않으면 삭제에 실패해야 한다.

        // given
        given(contentRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

        // when
        ContentNotFoundException exception = assertThrows(
                ContentNotFoundException.class,
                () -> contentService.deleteContent(createUser("owner@example.com"), UUID.randomUUID())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
    }

    @Test
    @DisplayName("타 사용자 콘텐츠 삭제 거부")
    void shouldThrowWhenDeletingAnotherUsersContent() {
        // 다른 사용자의 콘텐츠를 삭제하면 접근 거부 예외를 반환해야 한다.

        // given
        User owner = createUser("owner@example.com");
        User viewer = createUser("viewer@example.com");
        Content content = createContent(createGenerationRequest(owner, UUID.randomUUID()), UUID.randomUUID(), GenerationType.ALL);
        given(contentRepository.findByIdAndIsDeletedFalse(content.getId())).willReturn(Optional.of(content));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> contentService.deleteContent(viewer, content.getId())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
    }

    @Test
    @DisplayName("콘텐츠 재생성 시작 성공")
    void shouldStartRegeneration() {
        // 유효한 재생성 요청이면 processing ApiLog를 저장하고 비동기 재생성을 시작해야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID apiLogId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        Content originalContent = createContent(request, contentId, GenerationType.ALL);
        ApiLog savedLog = createApiLog(request, apiLogId, "gemini-2.5-flash", ApiStatus.PROCESSING);

        given(contentRepository.findByIdAndIsDeletedFalse(contentId)).willReturn(Optional.of(originalContent));
        given(geminiConfig.getModel()).willReturn("gemini-2.5-flash");
        given(apiLogRepository.save(any(ApiLog.class))).willReturn(savedLog);

        // when
        GenerateContentResponse response = testableContentService.startRegeneration(
                user,
                contentId,
                RegenerateContentRequest.builder()
                        .feedback("Make it more friendly")
                        .regenerateImage(true)
                        .target("INSTAGRAM")
                        .build()
        );

        // then
        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getTaskId()).isEqualTo(apiLogId);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(testableContentService.regenerateAsyncCalled).isTrue();
        assertThat(testableContentService.capturedRequestId).isEqualTo(requestId);
        assertThat(testableContentService.capturedOriginalContentId).isEqualTo(contentId);
        assertThat(testableContentService.capturedApiLogId).isEqualTo(apiLogId);
    }

    @Test
    @DisplayName("재생성 target 사전 차단")
    void shouldRejectInvalidTargetBeforeSavingApiLog() {
        // 지원하지 않는 target 값으로 재생성을 요청하면 ApiLog를 저장하지 않고 즉시 예외를 던져야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID contentId = UUID.randomUUID();
        GenerationRequest generationRequest = GenerationRequest.builder()
                .user(user)
                .concept("봄 프로모션")
                .build();
        ReflectionTestUtils.setField(generationRequest, "id", UUID.randomUUID());

        Content originalContent = Content.builder()
                .generationRequest(generationRequest)
                .generationType(GenerationType.ALL)
                .build();
        ReflectionTestUtils.setField(originalContent, "id", contentId);

        given(contentRepository.findByIdAndIsDeletedFalse(contentId)).willReturn(Optional.of(originalContent));

        RegenerateContentRequest request = RegenerateContentRequest.builder()
                .feedback("문구를 바꿔줘")
                .regenerateImage(true)
                .target("YOUTUBE")
                .build();

        // when
        InvalidRegenerationRequestException exception = assertThrows(
                InvalidRegenerationRequestException.class,
                () -> testableContentService.startRegeneration(user, contentId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REGENERATION_REQUEST);
        assertThat(testableContentService.regenerateAsyncCalled).isFalse();
        then(apiLogRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 재생성 시작 실패")
    void shouldThrowWhenStartingRegenerationForMissingContent() {
        // 원본 콘텐츠가 없으면 재생성 시작에 실패해야 한다.

        // given
        given(contentRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

        // when
        ContentNotFoundException exception = assertThrows(
                ContentNotFoundException.class,
                () -> testableContentService.startRegeneration(
                        createUser("owner@example.com"),
                        UUID.randomUUID(),
                        RegenerateContentRequest.builder().feedback("Change it").target("INSTAGRAM").build())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
        then(apiLogRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("타 사용자 콘텐츠 재생성 시작 거부")
    void shouldThrowWhenStartingRegenerationForAnotherUsersContent() {
        // 다른 사용자의 콘텐츠로 재생성을 시작하면 접근 거부 예외를 반환해야 한다.

        // given
        User owner = createUser("owner@example.com");
        User viewer = createUser("viewer@example.com");
        Content originalContent = createContent(createGenerationRequest(owner, UUID.randomUUID()), UUID.randomUUID(), GenerationType.ALL);
        given(contentRepository.findByIdAndIsDeletedFalse(originalContent.getId())).willReturn(Optional.of(originalContent));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> testableContentService.startRegeneration(
                        viewer,
                        originalContent.getId(),
                        RegenerateContentRequest.builder().feedback("Change it").target("INSTAGRAM").build())
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
        then(apiLogRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("인스타그램 재생성 비동기 성공")
    void shouldRegenerateInstagramOnly() throws Exception {
        // INSTAGRAM 재생성은 인스타그램 필드만 교체하고 나머지 플랫폼은 유지해야 한다.

        assertRegenerationSuccess("INSTAGRAM", "New instagram", List.of("#renewed"));
    }

    @Test
    @DisplayName("당근 재생성 비동기 성공")
    void shouldRegenerateKarrotOnly() throws Exception {
        // KARROT 재생성은 당근 필드만 교체하고 나머지 플랫폼은 유지해야 한다.

        assertRegenerationSuccess("KARROT", "New karrot", List.of("#market"));
    }

    @Test
    @DisplayName("네이버 재생성 비동기 성공")
    void shouldRegenerateNaverOnly() throws Exception {
        // NAVER 재생성은 네이버 필드만 교체하고 나머지 플랫폼은 유지해야 한다.

        assertRegenerationSuccess("NAVER", "New naver", List.of("#search"));
    }

    @Test
    @DisplayName("재생성 비동기 예외 치환")
    void shouldStoreSanitizedMessageWhenRegenerationFails() throws Exception {
        // 재생성 비동기 중 예상치 못한 예외가 나면 안전한 AI003 메시지로 치환해 저장해야 한다.

        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID apiLogId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        Content originalContent = createContent(request, contentId, GenerationType.ALL);
        ApiLog apiLog = createApiLog(request, apiLogId, "gemini-2.5-flash", ApiStatus.PROCESSING);
        CountDownLatch latch = new CountDownLatch(1);

        given(contentRepository.findById(contentId)).willReturn(Optional.of(originalContent));
        given(geminiService.generateProcessedRegenerationContent(any(GeminiRegenerationRequest.class)))
                .willReturn(Mono.error(new RuntimeException("https://example.test?key=secret")));
        given(apiLogRepository.findById(apiLogId)).willReturn(Optional.of(apiLog));
        given(apiLogRepository.save(any(ApiLog.class))).willAnswer(invocation -> {
            ApiLog saved = invocation.getArgument(0);
            if (saved.getStatus() == ApiStatus.ERROR) {
                latch.countDown();
            }
            return saved;
        });

        // when
        contentService.regenerateContentAsync(
                requestId,
                contentId,
                RegenerateContentRequest.builder().feedback("Change it").regenerateImage(true).target("INSTAGRAM").build(),
                apiLogId
        );

        awaitLatch(latch);

        // then
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(apiLog.getErrorMessage()).isEqualTo(ErrorCode.AI_REQUEST_FAILED.getMessage());
        then(contentImageRepository).should(never()).save(any(ContentImage.class));
    }

    @Test
    @DisplayName("예상치 못한 예외 메시지 치환")
    void shouldSanitizeUnexpectedTaskErrors() {
        // 예상치 못한 예외가 발생하면 원본 URL, key 파라미터 같은 민감한 값을 제거한 안전한 메시지만 반환해야 한다.

        // given
        String safeMessage = contentService.toSafeTaskErrorMessage(
                new RuntimeException("Gemini request failed: https://example.test?key=secret-value")
        );

        // then
        assertThat(safeMessage).isEqualTo(ErrorCode.AI_REQUEST_FAILED.getMessage());
        assertThat(safeMessage).doesNotContain("secret-value").doesNotContain("key=");
    }

    @Test
    @DisplayName("CustomException 메시지 유지")
    void shouldKeepKnownCustomExceptionMessage() {
        // 프로젝트에서 정의한 CustomException은 기존의 안전한 에러 메시지를 그대로 유지해야 한다.

        // given
        String safeMessage = contentService.toSafeTaskErrorMessage(new CustomException(ErrorCode.AI_PARSE_ERROR));

        // then
        assertThat(safeMessage).isEqualTo(ErrorCode.AI_PARSE_ERROR.getMessage());
    }

    private void assertRegenerationSuccess(String target, String regeneratedText, List<String> regeneratedHashtags) throws Exception {
        // given
        User user = createUser("owner@example.com");
        UUID requestId = UUID.randomUUID();
        UUID originalContentId = UUID.randomUUID();
        UUID regeneratedContentId = UUID.randomUUID();
        UUID apiLogId = UUID.randomUUID();
        GenerationRequest request = createGenerationRequest(user, requestId);
        Content originalContent = createContent(request, originalContentId, GenerationType.ALL);
        ContentImage image = createContentImage(originalContent, UUID.randomUUID(), UUID.randomUUID(), "stored/content-1.png");
        ApiLog apiLog = createApiLog(request, apiLogId, "gemini-2.5-flash", ApiStatus.PROCESSING);
        GeminiParsedResponse parsed = GeminiParsedResponse.builder()
                .text(regeneratedText)
                .hashtags(regeneratedHashtags)
                .inputTokens(2_000)
                .outputTokens(1_000)
                .responseTimeMs(654L)
                .build();
        CountDownLatch latch = new CountDownLatch(1);

        given(contentRepository.findById(originalContentId)).willReturn(Optional.of(originalContent));
        given(requestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(geminiService.generateProcessedRegenerationContent(any(GeminiRegenerationRequest.class))).willReturn(Mono.just(parsed));
        given(contentRepository.save(any(Content.class))).willAnswer(invocation -> {
            Content saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", regeneratedContentId);
            return saved;
        });
        given(contentImageRepository.findByContentOrderByCreatedAtAsc(originalContent)).willReturn(List.of(image));
        given(contentImageRepository.save(any(ContentImage.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(apiLogRepository.findById(apiLogId)).willReturn(Optional.of(apiLog));
        given(apiLogRepository.save(any(ApiLog.class))).willAnswer(invocation -> {
            ApiLog saved = invocation.getArgument(0);
            if (saved.getStatus() == ApiStatus.SUCCESS) {
                latch.countDown();
            }
            return saved;
        });

        // when
        contentService.regenerateContentAsync(
                requestId,
                originalContentId,
                RegenerateContentRequest.builder()
                        .feedback("Make it more playful")
                        .regenerateImage(true)
                        .target(target)
                        .build(),
                apiLogId
        );

        awaitLatch(latch);

        // then
        ArgumentCaptor<GeminiRegenerationRequest> requestCaptor = ArgumentCaptor.forClass(GeminiRegenerationRequest.class);
        then(geminiService).should().generateProcessedRegenerationContent(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTarget()).isEqualTo(target);
        if ("INSTAGRAM".equals(target)) {
            assertThat(requestCaptor.getValue().getOriginalText()).isEqualTo("Instagram body");
            assertThat(requestCaptor.getValue().getOriginalHashtags()).containsExactly("#insta");
        } else if ("KARROT".equals(target)) {
            assertThat(requestCaptor.getValue().getOriginalText()).isEqualTo("Karrot body");
            assertThat(requestCaptor.getValue().getOriginalHashtags()).containsExactly("#karrot");
        } else {
            assertThat(requestCaptor.getValue().getOriginalText()).isEqualTo("Naver body");
            assertThat(requestCaptor.getValue().getOriginalHashtags()).containsExactly("#naver");
        }

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        then(contentRepository).should().save(contentCaptor.capture());
        Content savedContent = contentCaptor.getValue();
        assertThat(savedContent.getGenerationType()).isEqualTo(GenerationType.TEXT_ONLY);
        if ("INSTAGRAM".equals(target)) {
            assertThat(savedContent.getInstagramText()).isEqualTo(regeneratedText);
            assertThat(savedContent.getInstagramHashtags()).containsExactlyElementsOf(regeneratedHashtags);
            assertThat(savedContent.getKarrotText()).isEqualTo("Karrot body");
            assertThat(savedContent.getNaverText()).isEqualTo("Naver body");
        } else if ("KARROT".equals(target)) {
            assertThat(savedContent.getKarrotText()).isEqualTo(regeneratedText);
            assertThat(savedContent.getKarrotTags()).containsExactlyElementsOf(regeneratedHashtags);
            assertThat(savedContent.getInstagramText()).isEqualTo("Instagram body");
            assertThat(savedContent.getNaverText()).isEqualTo("Naver body");
        } else {
            assertThat(savedContent.getNaverText()).isEqualTo(regeneratedText);
            assertThat(savedContent.getNaverKeywords()).containsExactlyElementsOf(regeneratedHashtags);
            assertThat(savedContent.getInstagramText()).isEqualTo("Instagram body");
            assertThat(savedContent.getKarrotText()).isEqualTo("Karrot body");
        }

        ArgumentCaptor<ContentImage> imageCaptor = ArgumentCaptor.forClass(ContentImage.class);
        then(contentImageRepository).should().save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getInputImageId()).isEqualTo(image.getInputImageId());
        assertThat(imageCaptor.getValue().getUrl()).isEqualTo(image.getUrl());
        then(gcsService).should(never()).copyFile(any(), any());
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(apiLog.getContent()).isNotNull();
        assertThat(apiLog.getContent().getId()).isEqualTo(regeneratedContentId);
        assertThat(apiLog.getCostUsd()).isEqualByComparingTo("0.0040000000");
    }

    private void awaitLatch(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    private User createUser(String email) {
        User user = new User(email, "테스트 사용자", "google", UUID.randomUUID().toString(), "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private GenerationRequest createGenerationRequest(User user, UUID requestId) {
        GenerationRequest request = GenerationRequest.builder()
                .user(user)
                .concept("Spring promotion")
                .additionalNote("Bright mood")
                .targetAge("20s")
                .targetGender("ALL")
                .build();
        ReflectionTestUtils.setField(request, "id", requestId);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.of(2026, 4, 14, 10, 0));
        ReflectionTestUtils.setField(request, "updatedAt", LocalDateTime.of(2026, 4, 14, 10, 5));
        return request;
    }

    private Store createStore(User user) {
        Store store = Store.builder()
                .user(user)
                .storeName("Today Cafe")
                .businessType("Cafe")
                .address("Seoul")
                .latitude(new BigDecimal("37.50000000"))
                .longitude(new BigDecimal("127.00000000"))
                .preferredStyle(PreferredStyle.CLEAN)
                .snsInstagram("@todaycafe")
                .build();
        ReflectionTestUtils.setField(store, "id", UUID.randomUUID());
        return store;
    }

    private InputImage createInputImage(GenerationRequest request, UUID imageId, String url, String description, int displayOrder) {
        InputImage image = InputImage.builder()
                .generationRequest(request)
                .url(url)
                .description(description)
                .displayOrder(displayOrder)
                .build();
        ReflectionTestUtils.setField(image, "id", imageId);
        ReflectionTestUtils.setField(image, "createdAt", LocalDateTime.of(2026, 4, 14, 10, 10 + displayOrder));
        return image;
    }

    private Content createContent(GenerationRequest request, UUID contentId, GenerationType generationType) {
        Content content = Content.builder()
                .generationRequest(request)
                .instagramText("Instagram body")
                .instagramHashtags(List.of("#insta"))
                .karrotText("Karrot body")
                .karrotTags(List.of("#karrot"))
                .naverText("Naver body")
                .naverKeywords(List.of("#naver"))
                .generationType(generationType)
                .aiModel("gemini-2.5-flash")
                .build();
        ReflectionTestUtils.setField(content, "id", contentId);
        ReflectionTestUtils.setField(content, "createdAt", LocalDateTime.of(2026, 4, 14, 11, 0));
        ReflectionTestUtils.setField(content, "updatedAt", LocalDateTime.of(2026, 4, 14, 11, 5));
        return content;
    }

    private ContentImage createContentImage(Content content, UUID imageId, UUID inputImageId, String url) {
        ContentImage image = ContentImage.builder()
                .content(content)
                .inputImageId(inputImageId)
                .url(url)
                .build();
        ReflectionTestUtils.setField(image, "id", imageId);
        ReflectionTestUtils.setField(image, "createdAt", LocalDateTime.of(2026, 4, 14, 11, 10));
        return image;
    }

    private ApiLog createApiLog(GenerationRequest request, UUID apiLogId, String model, ApiStatus status) {
        ApiLog apiLog = ApiLog.builder()
                .generationRequest(request)
                .model(model)
                .status(status)
                .build();
        ReflectionTestUtils.setField(apiLog, "id", apiLogId);
        return apiLog;
    }

    private static class TestableContentService extends ContentService {
        private boolean generateAsyncCalled;
        private boolean regenerateAsyncCalled;
        private UUID capturedRequestId;
        private UUID capturedOriginalContentId;
        private UUID capturedApiLogId;

        TestableContentService(
                ContentRepository contentRepository,
                ContentImageRepository contentImageRepository,
                ApiLogRepository apiLogRepository,
                GenerationRequestRepository requestRepository,
                InputImageRepository inputImageRepository,
                StoreRepository storeRepository,
                UserRepository userRepository,
                ContentPostRepository contentPostRepository,
                GeminiService geminiService,
                GcsService gcsService,
                TransactionTemplate transactionTemplate,
                GeminiConfig geminiConfig
        ) {
            super(
                    contentRepository,
                    contentImageRepository,
                    apiLogRepository,
                    requestRepository,
                    inputImageRepository,
                    storeRepository,
                    userRepository,
                    contentPostRepository,
                    geminiService,
                    gcsService,
                    transactionTemplate,
                    geminiConfig
            );
        }

        @Override
        public void generateContentAsync(UUID requestId, UUID apiLogId) {
            generateAsyncCalled = true;
            capturedRequestId = requestId;
            capturedApiLogId = apiLogId;
        }

        @Override
        public void regenerateContentAsync(UUID requestId, UUID originalContentId, RegenerateContentRequest regenerateRequest, UUID apiLogId) {
            regenerateAsyncCalled = true;
            capturedRequestId = requestId;
            capturedOriginalContentId = originalContentId;
            capturedApiLogId = apiLogId;
        }
    }
}
