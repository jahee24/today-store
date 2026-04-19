package today_store.content.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import today_store.authentication.entity.User;
import today_store.authentication.repository.UserRepository;
import today_store.common.config.GeminiConfig;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.common.gemini.service.GeminiService;
import today_store.content.content.dto.RegenerateContentRequest;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.GenerationType;
import today_store.content.content.exception.InvalidRegenerationRequestException;
import today_store.content.content.repository.ApiLogRepository;
import today_store.content.content.repository.ContentImageRepository;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.repository.ContentRepository;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.repository.GenerationRequestRepository;
import today_store.content.request.repository.InputImageRepository;
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

    private TestableContentService contentService;

    @BeforeEach
    void setUp() {
        contentService = new TestableContentService(
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
                () -> contentService.startRegeneration(user, contentId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REGENERATION_REQUEST);
        assertThat(contentService.regenerateAsyncCalled).isFalse();
        then(apiLogRepository).should(never()).save(any());
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

    private User createUser(String email) {
        User user = new User(email, "테스트 사용자", "google", UUID.randomUUID().toString(), "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private static class TestableContentService extends ContentService {
        private boolean regenerateAsyncCalled;

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
        public void regenerateContentAsync(UUID requestId, UUID originalContentId, RegenerateContentRequest regenerateRequest, UUID apiLogId) {
            regenerateAsyncCalled = true;
        }
    }
}
