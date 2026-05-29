package today_store.content.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import today_store.authentication.entity.User;
import today_store.common.gcs.GcsService;
import today_store.common.runcomfy.service.RunComfyService;
import today_store.content.content.entity.ApiLog;
import today_store.content.content.entity.ApiStatus;
import today_store.content.content.entity.InputImageVariation;
import today_store.content.content.event.TaskCompletedEvent;
import today_store.content.content.repository.ApiLogRepository;
import today_store.content.content.repository.InputImageVariationRepository;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;
import today_store.content.request.repository.InputImageRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Comfy 웹훅 서비스 테스트")
class ComfyWebhookServiceTest {

    @Mock
    private ApiLogRepository apiLogRepository;

    @Mock
    private InputImageRepository inputImageRepository;

    @Mock
    private InputImageVariationRepository variationRepository;

    @Mock
    private GcsService gcsService;

    @Mock
    private RunComfyService runComfyService;

    @Mock
    private WebClient webClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ComfyWebhookService comfyWebhookService;

    @BeforeEach
    void setUp() {
        comfyWebhookService = new ComfyWebhookService(
                apiLogRepository,
                inputImageRepository,
                variationRepository,
                gcsService,
                runComfyService,
                webClient,
                null,
                eventPublisher
        );
    }

    @Test
    @DisplayName("성공 확정 이벤트 발행")
    void shouldPublishTaskCompletedEventWhenFinalizeSuccess() {
        // 이미지 변형 작업 성공 확정 시 ApiLog를 SUCCESS로 저장하고 커밋 이후 이메일 알림이 가능하도록 완료 이벤트를 발행해야 한다.

        // given
        UUID apiLogId = UUID.randomUUID();
        UUID inputImageId = UUID.randomUUID();
        User user = createUser("owner@example.com");
        GenerationRequest request = createGenerationRequest(user);
        ApiLog apiLog = createApiLog(apiLogId, request, ApiStatus.PROCESSING);
        InputImage inputImage = createInputImage(inputImageId, request);
        InputImageVariation existingVariation = createVariation(inputImage, "Wide shot", "stored/existing.png");
        InputImageVariation duplicateVariation = createVariation(inputImage, "Wide shot", "stored/duplicate.png");
        InputImageVariation newVariation = createVariation(inputImage, "Close up", "stored/new.png");

        given(apiLogRepository.findByIdWithLock(apiLogId)).willReturn(Optional.of(apiLog));
        given(inputImageRepository.findById(inputImageId)).willReturn(Optional.of(inputImage));
        given(variationRepository.findByInputImageOrderByCreatedAtAsc(inputImage)).willReturn(List.of(existingVariation));

        // when
        comfyWebhookService.finalizeSuccess(apiLogId, inputImageId, List.of(duplicateVariation, newVariation));

        // then
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(apiLog.getCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        then(apiLogRepository).should().save(apiLog);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<InputImageVariation>> variationsCaptor = ArgumentCaptor.forClass(Iterable.class);
        then(variationRepository).should().saveAll(variationsCaptor.capture());
        assertThat(variationsCaptor.getValue()).containsExactly(newVariation);

        ArgumentCaptor<TaskCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getApiLogId()).isEqualTo(apiLogId);
        assertThat(eventCaptor.getValue().getUserEmail()).isEqualTo("owner@example.com");
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(eventCaptor.getValue().getTaskType()).isEqualTo("COMFY_UI_VARIATION_V1");
        assertThat(eventCaptor.getValue().getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("실패 확정 이벤트 발행")
    void shouldPublishTaskCompletedEventWhenFinalizeFailure() {
        // 이미지 변형 작업 실패 확정 시 에러 메시지를 저장하고 사용자에게 실패 알림을 보낼 수 있도록 완료 이벤트를 발행해야 한다.

        // given
        UUID apiLogId = UUID.randomUUID();
        ApiLog apiLog = createApiLog(apiLogId, createGenerationRequest(createUser("owner@example.com")), ApiStatus.PROCESSING);
        given(apiLogRepository.findByIdWithLock(apiLogId)).willReturn(Optional.of(apiLog));

        // when
        comfyWebhookService.finalizeFailure(apiLogId, "Verification failed or status mismatch");

        // then
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(apiLog.getErrorMessage()).isEqualTo("Verification failed or status mismatch");
        then(apiLogRepository).should().save(apiLog);

        ArgumentCaptor<TaskCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(eventCaptor.getValue().getErrorMessage()).isEqualTo("Verification failed or status mismatch");
        assertThat(eventCaptor.getValue().getUserEmail()).isEqualTo("owner@example.com");
    }

    @Test
    @DisplayName("중복 실패 확정 이벤트 미발행")
    void shouldNotPublishEventWhenApiLogAlreadyFinished() {
        // 이미 처리된 ApiLog를 중복 실패 처리할 때는 상태 저장과 이메일 이벤트 발행을 반복하지 않아야 한다.

        // given
        UUID apiLogId = UUID.randomUUID();
        ApiLog apiLog = createApiLog(apiLogId, createGenerationRequest(createUser("owner@example.com")), ApiStatus.PROCESSING);
        apiLog.completeSuccess(null, 0, 0, BigDecimal.ZERO, 0);
        given(apiLogRepository.findByIdWithLock(apiLogId)).willReturn(Optional.of(apiLog));

        // when
        comfyWebhookService.finalizeFailure(apiLogId, "late failure");

        // then
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.SUCCESS);
        then(apiLogRepository).should(never()).save(any());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("예외 경로 오류 이벤트 발행")
    void shouldPublishTaskCompletedEventWhenUpdatingApiLogToError() {
        // 비동기 처리 중 예외가 발생해 별도 트랜잭션으로 ERROR 상태를 저장하는 경로도 이메일 알림 이벤트를 누락하지 않아야 한다.

        // given
        UUID apiLogId = UUID.randomUUID();
        ApiLog apiLog = createApiLog(apiLogId, createGenerationRequest(createUser("owner@example.com")), ApiStatus.PROCESSING);
        given(apiLogRepository.findById(apiLogId)).willReturn(Optional.of(apiLog));

        // when
        comfyWebhookService.updateApiLogToError(apiLogId, "System Error: storage unavailable");

        // then
        assertThat(apiLog.getStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(apiLog.getErrorMessage()).isEqualTo("System Error: storage unavailable");
        then(apiLogRepository).should().save(apiLog);
        then(eventPublisher).should().publishEvent(any(TaskCompletedEvent.class));
    }

    private User createUser(String email) {
        User user = new User(email, "테스트 사용자", "google", UUID.randomUUID().toString(), "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private GenerationRequest createGenerationRequest(User user) {
        GenerationRequest request = GenerationRequest.builder()
                .user(user)
                .concept("테스트 컨셉")
                .build();
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        return request;
    }

    private ApiLog createApiLog(UUID apiLogId, GenerationRequest request, ApiStatus status) {
        ApiLog apiLog = ApiLog.builder()
                .generationRequest(request)
                .model("COMFY_UI_VARIATION_V1")
                .status(status)
                .build();
        ReflectionTestUtils.setField(apiLog, "id", apiLogId);
        ReflectionTestUtils.setField(apiLog, "createdAt", LocalDateTime.of(2026, 5, 15, 9, 30));
        return apiLog;
    }

    private InputImage createInputImage(UUID inputImageId, GenerationRequest request) {
        InputImage inputImage = InputImage.builder()
                .generationRequest(request)
                .url("stored/input.png")
                .description("원본 이미지")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(inputImage, "id", inputImageId);
        return inputImage;
    }

    private InputImageVariation createVariation(InputImage inputImage, String angleType, String url) {
        return InputImageVariation.builder()
                .inputImage(inputImage)
                .angleType(angleType)
                .url(url)
                .build();
    }
}
