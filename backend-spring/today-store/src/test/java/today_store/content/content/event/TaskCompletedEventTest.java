package today_store.content.content.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import today_store.authentication.entity.User;
import today_store.content.content.entity.ApiLog;
import today_store.content.content.entity.ApiStatus;
import today_store.content.request.entity.GenerationRequest;

@DisplayName("작업 완료 이벤트 테스트")
class TaskCompletedEventTest {

    @Test
    @DisplayName("ApiLog 필드 매핑")
    void shouldMapApiLogFieldsToEvent() {
        // 이메일 알림 리스너가 별도 조회 없이 사용할 수 있도록 ApiLog의 핵심 정보를 이벤트에 고정해야 한다.

        // given
        UUID apiLogId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 20);
        User user = new User("owner@example.com", "테스트 사용자", "google", "provider-id", "https://image.test/profile.png");
        GenerationRequest request = GenerationRequest.builder()
                .user(user)
                .concept("테스트 컨셉")
                .build();
        ApiLog apiLog = ApiLog.builder()
                .generationRequest(request)
                .model("COMFY_UI_VARIATION_V1")
                .status(ApiStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(apiLog, "id", apiLogId);
        ReflectionTestUtils.setField(apiLog, "createdAt", createdAt);
        apiLog.completeError("request timed out");

        // when
        TaskCompletedEvent event = new TaskCompletedEvent(this, apiLog);

        // then
        assertThat(event.getApiLogId()).isEqualTo(apiLogId);
        assertThat(event.getUserEmail()).isEqualTo("owner@example.com");
        assertThat(event.getStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(event.getTaskType()).isEqualTo("COMFY_UI_VARIATION_V1");
        assertThat(event.getErrorMessage()).isEqualTo("request timed out");
        assertThat(event.getSubmittedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("성공 이벤트 에러 없음")
    void shouldKeepErrorMessageNullForSuccessEvent() {
        // 성공 작업 알림에서는 실패 사유가 없어야 하므로 성공 상태와 null 에러 메시지를 그대로 전달해야 한다.

        // given
        User user = new User("owner@example.com", "테스트 사용자", "google", "provider-id", "https://image.test/profile.png");
        GenerationRequest request = GenerationRequest.builder()
                .user(user)
                .concept("테스트 컨셉")
                .build();
        ApiLog apiLog = ApiLog.builder()
                .generationRequest(request)
                .model("gemini-2.5-flash")
                .status(ApiStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(apiLog, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(apiLog, "createdAt", LocalDateTime.of(2026, 5, 15, 10, 20));
        apiLog.completeSuccess(null, 0, 0, BigDecimal.ZERO, 0);

        // when
        TaskCompletedEvent event = new TaskCompletedEvent(this, apiLog);

        // then
        assertThat(event.getStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(event.getTaskType()).isEqualTo("gemini-2.5-flash");
        assertThat(event.getErrorMessage()).isNull();
    }
}
