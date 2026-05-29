package today_store.common.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import today_store.authentication.entity.User;
import today_store.common.gcs.GcsService;
import today_store.content.content.entity.ApiLog;
import today_store.content.content.entity.ApiStatus;
import today_store.content.content.event.TaskCompletedEvent;
import today_store.content.request.entity.GenerationRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("작업 알림 리스너 테스트")
class TaskNotificationListenerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private GcsService gcsService;

    private TaskNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new TaskNotificationListener(emailService, templateEngine, notificationMapper,gcsService);
    }

    @Test
    @DisplayName("작업 완료 이메일 발송")
    void shouldSendEmailWhenTaskCompletedEventIsHandled() {
        // 작업 완료 이벤트를 받으면 내부 모델명과 오류 메시지를 알림 문구로 변환하고 템플릿 결과를 사용자 이메일로 발송해야 한다.

        // given
        TaskCompletedEvent event = new TaskCompletedEvent(this, createApiLog(ApiStatus.ERROR, "COMFY_UI_VARIATION_V1", "request timed out"));
        given(notificationMapper.translateTaskType("COMFY_UI_VARIATION_V1")).willReturn("이미지 변형");
        given(notificationMapper.translateErrorMessage("request timed out")).willReturn("작업 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
        given(templateEngine.process(eq("mail/task-notification"), org.mockito.ArgumentMatchers.any(IContext.class)))
                .willReturn("<html>알림 본문</html>");

        // when
        listener.handleTaskCompletedEvent(event);

        // then
        ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);
        then(templateEngine).should().process(eq("mail/task-notification"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("taskType")).isEqualTo("이미지 변형");
        assertThat(contextCaptor.getValue().getVariable("status")).isEqualTo("ERROR");
        assertThat(contextCaptor.getValue().getVariable("errorMessage"))
                .isEqualTo("작업 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(contextCaptor.getValue().getVariable("submittedAt")).isEqualTo("2026-05-15 09:30:00");
        then(emailService).should().sendTaskNotification(
                "owner@example.com",
                "[알림] 이미지 변형 작업이 완료되었습니다 (ERROR)",
                "<html>알림 본문</html>"
        );
    }

    @Test
    @DisplayName("알림 처리 예외 격리")
    void shouldNotSendEmailWhenTemplateProcessingFails() {
        // 알림 템플릿 처리 중 예외가 발생해도 이벤트 처리 예외를 밖으로 전파하지 않고 메인 작업 흐름을 보호해야 한다.

        // given
        TaskCompletedEvent event = new TaskCompletedEvent(this, createApiLog(ApiStatus.SUCCESS, "gemini-2.5-flash", null));
        given(notificationMapper.translateTaskType("gemini-2.5-flash")).willReturn("콘텐츠 생성");
        given(notificationMapper.translateErrorMessage(null)).willReturn("알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        given(templateEngine.process(eq("mail/task-notification"), org.mockito.ArgumentMatchers.any(IContext.class)))
                .willThrow(new IllegalStateException("template missing"));

        // when
        listener.handleTaskCompletedEvent(event);

        // then
        then(emailService).should(never()).sendTaskNotification(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private ApiLog createApiLog(ApiStatus status, String model, String errorMessage) {
        User user = new User("owner@example.com", "테스트 사용자", "google", "provider-id", "https://image.test/profile.png");
        GenerationRequest request = GenerationRequest.builder()
                .user(user)
                .concept("테스트 컨셉")
                .build();
        ApiLog apiLog = ApiLog.builder()
                .generationRequest(request)
                .model(model)
                .status(ApiStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(apiLog, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));
        ReflectionTestUtils.setField(apiLog, "createdAt", LocalDateTime.of(2026, 5, 15, 9, 30));
        if (status == ApiStatus.SUCCESS) {
            apiLog.completeSuccess(null, 0, 0, BigDecimal.ZERO, 0);
        } else if (status == ApiStatus.ERROR) {
            apiLog.completeError(errorMessage);
        }
        return apiLog;
    }
}
