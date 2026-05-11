package today_store.common.runcomfy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import today_store.common.config.RunComfyConfig;
import today_store.common.runcomfy.dto.RunComfyInferenceRequest;
import today_store.common.runcomfy.dto.RunComfyInferenceResponse;
import today_store.common.runcomfy.dto.RunComfyResultResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RunComfyService {

    private final RunComfyConfig runComfyConfig;
    private final WebClient webClient;

    public RunComfyService(RunComfyConfig runComfyConfig,
                           @Qualifier("runComfyWebClient") WebClient webClient) {
        this.runComfyConfig = runComfyConfig;
        this.webClient = webClient;
    }

    /**
     * 이미지 변형 요청 (Async Queue)
     *
     * @param imageUrl GCS에 업로드된 원본 이미지 URL (Public accessible)
     * @param apiLogId Webhook에서 식별을 위해 사용할 ID
     * @param inputImageId 변형의 모체가 된 원본 이미지 ID
     * @return requestId 등을 포함한 응답
     */
    public Mono<RunComfyInferenceResponse> requestVariation(String imageUrl, String apiLogId, String inputImageId) {
        String deploymentId = runComfyConfig.getDeploymentId();
        String webhookUrl = String.format("%s?apiLogId=%s&inputImageId=%s",
                runComfyConfig.getWebhookUrl(), apiLogId, inputImageId);

        Map<String, Object> imageOverride = new HashMap<>();
        imageOverride.put("inputs", Map.of("image", imageUrl));

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("25", imageOverride); // target_workflow.json에서 LoadImage 노드는 25번

        RunComfyInferenceRequest request = RunComfyInferenceRequest.builder()
                .overrides(overrides)
                .build();

        log.info("Requesting RunComfy variation for apiLogId: {}, deploymentId: {}", apiLogId, deploymentId);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/prod/v1/deployments/{deployment_id}/inference")
                        .queryParam("webhook", webhookUrl)
                        .queryParam("webhook_intermediate_status", true)
                        .build(deploymentId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RunComfyInferenceResponse.class)
                .doOnError(e -> log.error("Error requesting RunComfy variation for apiLogId: {}", apiLogId));
    }

    // 결과 조회
    public Mono<RunComfyResultResponse> getResult(String requestId) {
        String deploymentId = runComfyConfig.getDeploymentId();

        return webClient.get()
                .uri("/prod/v1/deployments/{deployment_id}/requests/{request_id}/result", deploymentId, requestId)
                .retrieve()
                .bodyToMono(RunComfyResultResponse.class)
                .doOnError(e -> log.error("Error getting RunComfy result for requestId: {}", requestId));
    }
}
