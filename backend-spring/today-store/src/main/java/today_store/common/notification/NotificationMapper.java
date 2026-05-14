package today_store.common.notification;

import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public String translateTaskType(String taskType) {
        if (taskType == null) return "AI 작업";

        if (taskType.toLowerCase().contains("gemini")) {
            return "콘텐츠 생성";
        }
        if ("COMFY_UI_VARIATION_V1".equals(taskType)) {
            return "이미지 변형";
        }

        return "AI 작업";
    }

    public String translateErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        }

        String lowerMessage = errorMessage.toLowerCase();

        if (lowerMessage.contains("timeout") || lowerMessage.contains("timed out")) {
            return "작업 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
        }
        if (lowerMessage.contains("verification failed") || lowerMessage.contains("status mismatch")) {
            return "작업 결과 검증에 실패했습니다. 관리자에게 문의해 주세요.";
        }
        if (lowerMessage.contains("failed to initiate") || lowerMessage.contains("request failed")) {
            return "AI 서비스 요청 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        }
        if (lowerMessage.contains("image processing error") || lowerMessage.contains("upload variation")) {
            return "이미지 처리 또는 저장 중 오류가 발생했습니다.";
        }

        // 기본 메시지
        return "처리 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }
}