package today_store.content.content.service;

import today_store.common.runcomfy.dto.RunComfyResultResponse;

import java.util.UUID;

public interface ComfyWebhookService {
    /**
     * Webhook 요청을 수신하고 즉시 응답하기 위한 진입점입니다.
     */
    void handleWebhook(UUID apiLogId, UUID inputImageId, RunComfyResultResponse payload);

    /**
     * 실제 무거운 작업을 비동기로 처리합니다.
     */
    void processWebhookTask(UUID apiLogId, UUID inputImageId, RunComfyResultResponse payload);
}
