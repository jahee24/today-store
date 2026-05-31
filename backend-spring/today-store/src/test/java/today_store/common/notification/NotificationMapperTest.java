package today_store.common.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("알림 매퍼 테스트")
class NotificationMapperTest {

    private final NotificationMapper notificationMapper = new NotificationMapper();

    @Test
    @DisplayName("작업 타입 한글 변환")
    void shouldTranslateTaskType() {
        // 외부 AI 모델 식별자를 사용자에게 노출하지 않고 알림용 한글 작업명으로 변환해야 한다.

        // given

        // when & then
        assertThat(notificationMapper.translateTaskType("gemini-2.5-flash")).isEqualTo("콘텐츠 생성");
        assertThat(notificationMapper.translateTaskType("COMFY_UI_VARIATION_V1")).isEqualTo("이미지 변형");
        assertThat(notificationMapper.translateTaskType(null)).isEqualTo("AI 작업");
        assertThat(notificationMapper.translateTaskType("unknown-model")).isEqualTo("AI 작업");
    }

    @Test
    @DisplayName("에러 메시지 한글 변환")
    void shouldTranslateErrorMessage() {
        // 시스템 내부 오류 문구를 그대로 보내지 않고 사용자가 이해할 수 있는 안전한 메시지로 변환해야 한다.

        // given

        // when & then
        assertThat(notificationMapper.translateErrorMessage(null))
                .isEqualTo("알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(notificationMapper.translateErrorMessage("request timed out"))
                .isEqualTo("작업 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(notificationMapper.translateErrorMessage("Verification failed"))
                .isEqualTo("작업 결과 검증에 실패했습니다. 관리자에게 문의해 주세요.");
        assertThat(notificationMapper.translateErrorMessage("failed to initiate comfy task"))
                .isEqualTo("AI 서비스 요청 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(notificationMapper.translateErrorMessage("upload variation failed"))
                .isEqualTo("이미지 처리 또는 저장 중 오류가 발생했습니다.");
        assertThat(notificationMapper.translateErrorMessage("unexpected provider message"))
                .isEqualTo("처리 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
