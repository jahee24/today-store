package today_store.content.content.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.common.runcomfy.dto.RunComfyResultResponse;
import today_store.common.runcomfy.service.RunComfyService;
import today_store.content.content.exception.TaskNotFoundException;
import today_store.content.request.exception.GenerationRequestNotFoundException;
import today_store.content.content.entity.ApiLog;
import today_store.content.content.entity.ApiStatus;
import today_store.content.content.entity.InputImageVariation;
import today_store.content.content.repository.ApiLogRepository;
import today_store.content.content.repository.InputImageVariationRepository;
import today_store.content.request.entity.InputImage;
import today_store.content.request.repository.InputImageRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ComfyWebhookService {

    private final ApiLogRepository apiLogRepository;
    private final InputImageRepository inputImageRepository;
    private final InputImageVariationRepository variationRepository;
    private final GcsService gcsService;
    private final RunComfyService runComfyService;
    private final WebClient webClient;
    private final ComfyWebhookService self;

    public ComfyWebhookService(
            ApiLogRepository apiLogRepository,
            InputImageRepository inputImageRepository,
            InputImageVariationRepository variationRepository,
            GcsService gcsService,
            RunComfyService runComfyService,
            WebClient webClient,
            @Lazy ComfyWebhookService self) {
        this.apiLogRepository = apiLogRepository;
        this.inputImageRepository = inputImageRepository;
        this.variationRepository = variationRepository;
        this.gcsService = gcsService;
        this.runComfyService = runComfyService;
        this.webClient = webClient;
        this.self = self;
    }

    private static final Map<String, String> NODE_ANGLE_MAP = Map.of(
            "31", "Close up",
            "34", "Wide shot",
            "36", "45 right",
            "38", "90 right",
            "41", "Aerial view",
            "43", "Low angle",
            "45", "45 left",
            "47", "90 left"
    );

    public void handleWebhook(UUID apiLogId, UUID inputImageId, RunComfyResultResponse payload) {
        log.info("Webhook received for apiLogId: {}, status: {}. Returning immediate response.", apiLogId, payload.getStatus());

        // 상태 체크 (동기) - 락 없이 현재 상태 확인
        ApiLog apiLog = apiLogRepository.findById(apiLogId)
                .orElseThrow(TaskNotFoundException::new);

        if (apiLog.getStatus() != ApiStatus.PROCESSING) {
            log.warn("Webhook ignored: ApiLog {} is already in {} state.", apiLogId, apiLog.getStatus());
            return;
        }

        // 비동기 작업 실행
        self.processWebhookTask(apiLogId, inputImageId, payload);
    }

    @Async
    public void processWebhookTask(UUID apiLogId, UUID inputImageId, RunComfyResultResponse payload) {
        log.info("Starting asynchronous processing for apiLogId: {}", apiLogId);

        try {
            // [Step 1] 사전 검증 (No Lock)
            ApiLog apiLog = apiLogRepository.findById(apiLogId)
                    .orElseThrow(TaskNotFoundException::new);

            if (apiLog.getStatus() != ApiStatus.PROCESSING) {
                log.info("ApiLog {} is already finished with status: {}. Skipping.", apiLogId, apiLog.getStatus());
                return;
            }

            // Security check
            if (apiLog.getExternalRequestId() != null && !apiLog.getExternalRequestId().equals(payload.getRequestId())) {
                log.error("Security Alert: RequestId mismatch for apiLog {}", apiLogId);
                return;
            }

            // [Step 2] I/O 작업 수행 (No Lock)
            if ("succeeded".equalsIgnoreCase(payload.getStatus())) {
                log.info("Verifying success via RunComfy API for apiLog: {}", apiLogId);
                RunComfyResultResponse verifiedPayload = runComfyService.getResult(apiLog.getExternalRequestId()).block();

                if (verifiedPayload != null && "succeeded".equalsIgnoreCase(verifiedPayload.getStatus())) {
                    List<InputImageVariation> variations = collectVariations(inputImageId, verifiedPayload);
                    // [Step 3] 최종 상태 반영 (Short Lock)
                    self.finalizeSuccess(apiLogId, inputImageId, variations);
                } else {
                    self.finalizeFailure(apiLogId, "Verification failed or status mismatch");
                }
            } else if ("failed".equalsIgnoreCase(payload.getStatus())) {
                String error = payload.getError() != null ? payload.getError().getError() : "Unknown failure";
                self.finalizeFailure(apiLogId, error);
            }

        } catch (Exception e) {
            log.error("Fatal error in asynchronous webhook processing for apiLogId: {}", apiLogId, e);
            self.updateApiLogToError(apiLogId, "System Error: " + e.getMessage());
        }
    }

    private List<InputImageVariation> collectVariations(UUID inputImageId, RunComfyResultResponse payload) {
        InputImage inputImage = inputImageRepository.findById(inputImageId)
                .orElseThrow(GenerationRequestNotFoundException::new);

        List<InputImageVariation> variationsToSave = new ArrayList<>();
        if (payload.getOutputs() != null) {
            for (Map.Entry<String, String> entry : NODE_ANGLE_MAP.entrySet()) {
                String nodeId = entry.getKey();
                String angleName = entry.getValue();

                RunComfyResultResponse.NodeOutput nodeOutput = payload.getOutputs().get(nodeId);
                if (nodeOutput != null && nodeOutput.getImages() != null) {
                    for (RunComfyResultResponse.ImageInfo imageInfo : nodeOutput.getImages()) {
                        InputImageVariation variation = uploadToGcs(inputImage, angleName, imageInfo);
                        if (variation != null) {
                            variationsToSave.add(variation);
                        }
                    }
                }
            }
        }
        return variationsToSave;
    }

    @Transactional
    public void finalizeSuccess(UUID apiLogId, UUID inputImageId, List<InputImageVariation> variations) {
        // [Final Lock] 상태 변경 직전에만 락 획득
        ApiLog apiLog = apiLogRepository.findByIdWithLock(apiLogId)
                .orElseThrow(TaskNotFoundException::new);

        if (apiLog.getStatus() != ApiStatus.PROCESSING) {
            log.warn("ApiLog {} already processed by another thread. Status: {}", apiLogId, apiLog.getStatus());
            return;
        }

        InputImage inputImage = inputImageRepository.findById(inputImageId)
                .orElseThrow(GenerationRequestNotFoundException::new);

        // 중복 각도 체크 후 저장
        Set<String> existingAngles = variationRepository.findByInputImageOrderByCreatedAtAsc(inputImage)
                .stream().map(InputImageVariation::getAngleType).collect(Collectors.toSet());

        List<InputImageVariation> newVariations = variations.stream()
                .filter(v -> !existingAngles.contains(v.getAngleType()))
                .collect(Collectors.toList());

        if (!newVariations.isEmpty()) {
            variationRepository.saveAll(newVariations);
            log.info("Saved {} new variations for apiLog {}", newVariations.size(), apiLogId);
        }

        apiLog.completeSuccess(null, 0, 0, BigDecimal.ZERO, 0);
        apiLogRepository.save(apiLog);
    }

    @Transactional
    public void finalizeFailure(UUID apiLogId, String errorMessage) {
        ApiLog apiLog = apiLogRepository.findByIdWithLock(apiLogId)
                .orElseThrow(TaskNotFoundException::new);

        if (apiLog.getStatus() == ApiStatus.PROCESSING) {
            apiLog.completeError(errorMessage);
            apiLogRepository.save(apiLog);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateApiLogToError(UUID apiLogId, String errorMessage) {
        log.info("Updating ApiLog {} to ERROR state via separate transaction. Message: {}", apiLogId, errorMessage);
        apiLogRepository.findById(apiLogId).ifPresent(apiLog -> {
            apiLog.completeError(errorMessage);
            apiLogRepository.save(apiLog);
        });
    }

    private InputImageVariation uploadToGcs(InputImage inputImage, String angleType, RunComfyResultResponse.ImageInfo imageInfo) {
        String imageUrl = imageInfo.getUrl();
        log.info("Downloading image from RunComfy filename: {} for angle: {}", imageInfo.getFilename(), angleType);

        try {
            byte[] imageBytes = webClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (imageBytes != null) {
                String gcsPath = gcsService.uploadFile(imageBytes, "image/png", "variations", imageInfo.getFilename());

                return InputImageVariation.builder()
                        .inputImage(inputImage)
                        .url(gcsPath)
                        .angleType(angleType)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to upload variation to GCS for angle: {}", angleType);
            throw new CustomException(ErrorCode.AI_IMAGE_PROCESSING_ERROR, "Failed to upload variation for angle: " + angleType);
        }
        return null;
    }
}
