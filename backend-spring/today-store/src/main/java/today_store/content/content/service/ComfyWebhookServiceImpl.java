package today_store.content.content.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import today_store.common.exception.CustomException;
import today_store.content.content.exception.TaskNotFoundException;
import today_store.content.request.exception.GenerationRequestNotFoundException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.common.runcomfy.dto.RunComfyResultResponse;
import today_store.common.runcomfy.service.RunComfyService;
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
public class ComfyWebhookServiceImpl implements ComfyWebhookService {

    private final ApiLogRepository apiLogRepository;
    private final InputImageRepository inputImageRepository;
    private final InputImageVariationRepository variationRepository;
    private final GcsService gcsService;
    private final RunComfyService runComfyService;
    private final WebClient webClient;
    private final TransactionTemplate transactionTemplate;
    private final ComfyWebhookService self;

    public ComfyWebhookServiceImpl(
            ApiLogRepository apiLogRepository,
            InputImageRepository inputImageRepository,
            InputImageVariationRepository variationRepository,
            GcsService gcsService,
            RunComfyService runComfyService,
            WebClient webClient,
            TransactionTemplate transactionTemplate,
            @Lazy ComfyWebhookService self) {
        this.apiLogRepository = apiLogRepository;
        this.inputImageRepository = inputImageRepository;
        this.variationRepository = variationRepository;
        this.gcsService = gcsService;
        this.runComfyService = runComfyService;
        this.webClient = webClient;
        this.transactionTemplate = transactionTemplate;
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

    @Override
    public void handleWebhook(UUID apiLogId, UUID inputImageId, RunComfyResultResponse payload) {
        log.info("Webhook received for apiLogId: {}, status: {}. Returning immediate response.", apiLogId, payload.getStatus());

        // 최소한의 상태 체크 (동기) - 비관적 락 없이 현재 상태만 가볍게 확인
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
    @Override
    public void processWebhookTask(UUID apiLogId, UUID inputImageId, RunComfyResultResponse payload) {
        log.info("Starting asynchronous processing for apiLogId: {}", apiLogId);

        transactionTemplate.execute(status -> {
            try {
                ApiStatusCheck(apiLogId, inputImageId, payload);
            } catch (Exception e) {
                log.error("Error in asynchronous webhook processing for apiLogId: {}", apiLogId, e);
            }
            return null;
        });
    }

    private void ApiStatusCheck(UUID apiLogId, UUID inputImageId, RunComfyResultResponse payload) {
        // [Step 2] 비관적 락을 사용하여 동시성 제어 (Race Condition 방지)
        ApiLog apiLog = apiLogRepository.findByIdWithLock(apiLogId)
                .orElseThrow(TaskNotFoundException::new);

        // [Step 1] 멱등성 체크: 이미 처리된 작업(SUCCESS, ERROR 등)은 중복 처리하지 않음
        if (apiLog.getStatus() != ApiStatus.PROCESSING) {
            log.warn("Webhook ignored: ApiLog {} is already in {} state. Duplicate or late webhook.",
                    apiLogId, apiLog.getStatus());
            return;
        }

        if ("succeeded".equalsIgnoreCase(payload.getStatus())) {
            // [Security] Reverse Verification: Webhook 페이로드를 그대로 믿지 않고 API로 재조회
            log.info("Verifying success status via RunComfy API for requestId: {}", payload.getRequestId());
            RunComfyResultResponse verifiedPayload = runComfyService.getResult(payload.getRequestId()).block();

            if (verifiedPayload != null && "succeeded".equalsIgnoreCase(verifiedPayload.getStatus())) {
                processSuccess(apiLog, inputImageId, verifiedPayload);
            } else {
                log.error("Reverse verification failed for requestId: {}. Status from API: {}",
                        payload.getRequestId(), verifiedPayload != null ? verifiedPayload.getStatus() : "null");
                processFailure(apiLog, payload);
            }
        } else if ("failed".equalsIgnoreCase(payload.getStatus())) {
            processFailure(apiLog, payload);
        } else {
            log.info("Skipping intermediate status: {} for ApiLog {}", payload.getStatus(), apiLogId);
        }
    }

    private void processSuccess(ApiLog apiLog, UUID inputImageId, RunComfyResultResponse payload) {
        try {
            InputImage inputImage = inputImageRepository.findById(inputImageId)
                    .orElseThrow(GenerationRequestNotFoundException::new);

            // [Step 3] 멱등성 보강: 이미 저장된 변형 각도 수집
            Set<String> existingAngles = variationRepository.findByInputImageOrderByCreatedAtAsc(inputImage)
                    .stream().map(InputImageVariation::getAngleType).collect(Collectors.toSet());

            List<InputImageVariation> variationsToSave = new ArrayList<>();

            if (payload.getOutputs() != null) {
                for (Map.Entry<String, String> entry : NODE_ANGLE_MAP.entrySet()) {
                    String nodeId = entry.getKey();
                    String angleName = entry.getValue();

                    // 이미 해당 각도의 변형이 저장되어 있다면 건너뜀
                    if (existingAngles.contains(angleName)) {
                        log.info("Skipping existing variation angle: {} for inputImage: {}", angleName, inputImageId);
                        continue;
                    }

                    RunComfyResultResponse.NodeOutput nodeOutput = payload.getOutputs().get(nodeId);
                    if (nodeOutput != null && nodeOutput.getImages() != null) {
                        for (RunComfyResultResponse.ImageInfo imageInfo : nodeOutput.getImages()) {
                            // [Step 3] 원자성 보장: GCS 업로드만 먼저 수행하고 객체 생성
                            InputImageVariation variation = uploadToGcs(inputImage, angleName, imageInfo);
                            if (variation != null) {
                                variationsToSave.add(variation);
                            }
                        }
                    }
                }
            }

            // 모든 이미지가 정상적으로 준비되었을 때만 일괄 저장 (Atomicity)
            if (!variationsToSave.isEmpty()) {
                variationRepository.saveAll(variationsToSave);
                log.info("Successfully saved {} new variations for inputImage: {}", variationsToSave.size(), inputImageId);
            }

            // ApiLog 완료 처리 (Content 없이 성공 처리)
            apiLog.completeSuccess(null, 0, 0, BigDecimal.ZERO, 0);
            apiLogRepository.save(apiLog);


        } catch (Exception e) {
            log.error("Error processing successful ComfyUI variation for apiLogId: {}", apiLog.getId());
            // [Step 3] 상세 오류 기록
            apiLog.completeError("Processing failed at variation collection/upload: " + e.getMessage());
            apiLogRepository.save(apiLog);
            throw e; // 트랜잭션 롤백 유도
        }
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

    private void processFailure(ApiLog apiLog, RunComfyResultResponse payload) {
        String errorMessage = "RunComfy failed";
        if (payload.getError() != null) {
            errorMessage = payload.getError().getError() + ": " + payload.getError().getDetails();
        }
        apiLog.completeError(errorMessage);
        apiLogRepository.save(apiLog);
    }
}
