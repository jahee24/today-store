package today_store.content.content.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import today_store.authentication.entity.User;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.authentication.repository.UserRepository;
import today_store.common.config.GeminiConfig;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.common.gemini.dto.GeminiPromptRequest;
import today_store.common.gemini.dto.GeminiRegenerationRequest;
import today_store.common.gemini.service.GeminiService;
import today_store.common.runcomfy.service.RunComfyService;
import today_store.content.content.dto.*;
import today_store.content.content.entity.*;
import today_store.content.content.exception.EmptyImageListException;
import today_store.content.content.repository.*;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;
import today_store.content.request.repository.GenerationRequestRepository;
import today_store.content.request.repository.InputImageRepository;
import today_store.store.entity.Store;
import today_store.store.repository.StoreRepository;
import today_store.store.exception.StoreNotFoundException;
import today_store.content.request.exception.GenerationRequestNotFoundException;
import today_store.content.content.exception.TaskNotFoundException;
import today_store.content.content.exception.ContentNotFoundException;
import today_store.authentication.exception.UserNotFoundException;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentImageRepository contentImageRepository;
    private final ApiLogRepository apiLogRepository;
    private final GenerationRequestRepository requestRepository;
    private final InputImageRepository inputImageRepository;
    private final InputImageVariationRepository inputImageVariationRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ContentPostRepository contentPostRepository;
    private final GeminiService geminiService;
    private final RunComfyService runComfyService;
    private final GcsService gcsService;
    private final TransactionTemplate transactionTemplate;
    private final GeminiConfig geminiConfig;

    @Transactional
    public GenerateContentResponse startGeneration(User user, UUID requestId) {
        GenerationRequest request = requestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(GenerationRequestNotFoundException::new);

        if (!request.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        ApiLog apiLog = ApiLog.builder()
                .generationRequest(request)
                .model(geminiConfig.getModel())
                .status(ApiStatus.PROCESSING)
                .build();

        ApiLog savedLog = apiLogRepository.save(apiLog);
        generateContentAsync(request.getId(), savedLog.getId());

        return GenerateContentResponse.builder()
                .requestId(requestId)
                .taskId(savedLog.getId())
                .startedAt(LocalDateTime.now())
                .build();
    }

    @Async
    public void generateContentAsync(UUID requestId, UUID apiLogId) {
        log.info("Starting background generation for request: {}, apiLog: {}", requestId, apiLogId);

        Mono.fromCallable(() -> {
                    return transactionTemplate.execute(status -> {
                        GenerationRequest request = requestRepository.findById(requestId).orElseThrow();

                        List<InputImage> inputImages = inputImageRepository.findByGenerationRequestOrderByDisplayOrderAsc(request);
                        List<String> signedUrls = inputImages.stream()
                                .map(img -> gcsService.generateSignedUrl(img.getUrl()))
                                .collect(Collectors.toList());
                        List<String> imageDescriptions = inputImages.stream()
                                .map(InputImage::getDescription)
                                .collect(Collectors.toList());

                        Store store = storeRepository.findByUser(request.getUser())
                                .orElseThrow(StoreNotFoundException::new);

                        return new GeminiPromptRequest(
                                store.getStoreName(),
                                store.getBusinessType(),
                                store.getAddress(),
                                store.getLatitude() != null ? store.getLatitude().doubleValue() : null,
                                store.getLongitude() != null ? store.getLongitude().doubleValue() : null,
                                request.getTargetAge(),
                                request.getTargetGender(),
                                request.getConcept(),
                                request.getAdditionalNote(),
                                imageDescriptions,
                                signedUrls
                        );
                    });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(geminiService::generateProcessedContent)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(parsed -> {
                    transactionTemplate.execute(status -> {
                        ApiLog apiLog = apiLogRepository.findById(apiLogId).orElseThrow();
                        GenerationRequest request = requestRepository.findById(requestId).orElseThrow();
                        List<InputImage> inputImages = inputImageRepository.findByGenerationRequestOrderByDisplayOrderAsc(request);

                        // Save Content
                        Content content = Content.builder()
                                .generationRequest(request)
                                .instagramText(parsed.getText())
                                .instagramHashtags(parsed.getHashtags() != null ? parsed.getHashtags() : List.of())
                                .karrotText(parsed.getText())
                                .karrotTags(parsed.getHashtags() != null ? parsed.getHashtags() : List.of())
                                .naverText(parsed.getText())
                                .naverKeywords(parsed.getHashtags() != null ? parsed.getHashtags() : List.of())
                                .generationType(GenerationType.ALL)
                                .aiModel(apiLog.getModel())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        Content savedContent = contentRepository.save(content);

                        // Save ContentImages (map from input images)
                        for (InputImage inputImage : inputImages) {
                            String snapshotUrl = gcsService.copyFile(inputImage.getUrl(), "contents");
                            ContentImage contentImage = ContentImage.builder()
                                    .content(savedContent)
                                    .inputImageId(inputImage.getId())
                                    .url(snapshotUrl)
                                    .build();
                            contentImageRepository.save(contentImage);
                        }

                        // Update ApiLog with Metadata from parsed response
                        Integer inputTokens = parsed.getInputTokens();
                        Integer outputTokens = parsed.getOutputTokens();
                        BigDecimal costUsd = calculateCost(inputTokens, outputTokens);
                        Integer responseTimeMs = parsed.getResponseTimeMs() != null ? parsed.getResponseTimeMs().intValue() : null;

                        apiLog.completeSuccess(savedContent, inputTokens, outputTokens, costUsd, responseTimeMs);
                        apiLogRepository.save(apiLog);
                        return null;
                    });
                })
                .doOnError(e -> {
                    String safeErrorMessage = toSafeTaskErrorMessage(e);
                    log.error("Error during background generation for apiLog {} [{}]: {}",
                            apiLogId,
                            Exceptions.unwrap(e).getClass().getSimpleName(),
                            safeErrorMessage);
                    transactionTemplate.execute(status -> {
                        ApiLog apiLog = apiLogRepository.findById(apiLogId).orElseThrow();
                        apiLog.completeError(safeErrorMessage);
                        apiLogRepository.save(apiLog);
                        return null;
                    });
                })
                .subscribe();
    }

    @Transactional(readOnly = true)
    public TaskStatusResponse getTaskStatus(User user, UUID apiLogId) {
        ApiLog apiLog = apiLogRepository.findById(apiLogId)
                .orElseThrow(TaskNotFoundException::new);

        if (!apiLog.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        String result = apiLog.getContent() != null ? apiLog.getContent().getId().toString() : null;

        return TaskStatusResponse.builder()
                .taskId(apiLog.getId())
                .status(apiLog.getStatus().name().toLowerCase())
                .result(result)
                .errorMessage(apiLog.getErrorMessage())
                .build();
    }

    @Transactional(readOnly = true)
    public ContentResponse getContent(User user, UUID contentId) {
        Content content = contentRepository.findByIdAndIsDeletedFalse(contentId)
                .orElseThrow(ContentNotFoundException::new);

        if (!content.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        List<ContentImage> images = contentImageRepository.findByContentOrderByCreatedAtAsc(content);

        boolean isPosted = contentPostRepository.existsByContentAndStatus(content, PublishStatus.COMPLETED);

        return ContentResponse.from(content, images, isPosted, gcsService::generateSignedUrl);
    }

    @Transactional(readOnly = true)
    public ContentListResponse getContentList(User user, UUID requestId) {
        GenerationRequest request = requestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(GenerationRequestNotFoundException::new);

        if (!request.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        List<Content> contents = contentRepository.findByGenerationRequestAndIsDeletedFalseOrderByCreatedAtDesc(request);

        Set<UUID> postedContentIds = new HashSet<>();
        if (!contents.isEmpty()) {
            postedContentIds.addAll(contentPostRepository.findContentIdsByContentInAndStatus(contents, PublishStatus.COMPLETED));
        }

        return ContentListResponse.from(requestId, contents, postedContentIds);
    }

    @Transactional
    public ContentResponse updateContent(String email, UUID contentId, UpdateContentRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        Content content = contentRepository.findByIdAndIsDeletedFalse(contentId)
                .orElseThrow(ContentNotFoundException::new);

        if (!content.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        if (request.getInstagram() != null) {
            content.updateInstagram(request.getInstagram().getText(), request.getInstagram().getHashtags());
        }
        if (request.getKarrot() != null) {
            content.updateKarrot(request.getKarrot().getText(), request.getKarrot().getHashtags());
        }
        if (request.getNaver() != null) {
            content.updateNaver(request.getNaver().getText(), request.getNaver().getHashtags());
        }

        Content updated = contentRepository.saveAndFlush(content);
        List<ContentImage> images = contentImageRepository.findByContentOrderByCreatedAtAsc(updated);
        boolean isPosted = contentPostRepository.existsByContentAndStatus(updated, PublishStatus.COMPLETED);
        return ContentResponse.from(updated, images, isPosted, gcsService::generateSignedUrl);
    }

    @Transactional
    public void deleteContent(User user, UUID contentId) {
        Content content = contentRepository.findByIdAndIsDeletedFalse(contentId)
                .orElseThrow(ContentNotFoundException::new);

        if (!content.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        content.delete();
        contentRepository.save(content);
    }

    @Transactional
    public ContentResponse updateContentImages(User user, UUID contentId, UpdateContentImagesRequest request) {
        if (request.getImages() == null || request.getImages().isEmpty()) {
            throw new EmptyImageListException();
        }

        Content content = contentRepository.findByIdAndIsDeletedFalse(contentId).orElseThrow(ContentNotFoundException::new);
        if (!content.getGenerationRequest().getUser().getId().equals(user.getId())) throw new AccessDeniedToResourceException();

        // 1. Collect current URLs for potential cleanup
        List<ContentImage> currentImages = contentImageRepository.findByContentOrderByDisplayOrderAsc(content);
        Set<String> urlsBeforeUpdate = currentImages.stream()
                .map(ContentImage::getUrl)
                .collect(Collectors.toSet());

        // 기존 이미지 정보를 맵에 저장 (EXISTING 타입 재사용을 위함)
        Map<UUID, ContentImage> existingImagesMap = currentImages.stream()
                .collect(Collectors.toMap(ContentImage::getId, img -> img));

        // 기존 매핑 삭제
        contentImageRepository.deleteByContent(content);

        Set<String> urlsToKeep = new HashSet<>();
        int order = 0;
        for (UpdateContentImagesRequest.ImageSourceItem item : request.getImages()) {
            String urlToSnapshot;
            UUID inputImageId;

            switch (item.getType()) {
                case EXISTING:
                    ContentImage existing = existingImagesMap.get(item.getId());
                    if (existing == null) {
                        throw new AccessDeniedToResourceException();
                    }
                    urlToSnapshot = existing.getUrl();
                    inputImageId = existing.getInputImageId();
                    break;
                case VARIATION:
                    InputImageVariation variation = inputImageVariationRepository.findById(item.getId())
                            .orElseThrow(() -> new RuntimeException("Variation not found: " + item.getId()));

                    // Ownership check: variation must belong to the same generation request as the content
                    if (!variation.getInputImage().getGenerationRequest().getId().equals(content.getGenerationRequest().getId())) {
                        throw new AccessDeniedToResourceException();
                    }

                    urlToSnapshot = variation.getUrl();
                    inputImageId = variation.getInputImage().getId();
                    break;
                case ORIGINAL:
                    InputImage original = inputImageRepository.findById(item.getId())
                            .orElseThrow(() -> new RuntimeException("Original image not found: " + item.getId()));

                    // Ownership check: original image must belong to the same generation request as the content
                    if (!original.getGenerationRequest().getId().equals(content.getGenerationRequest().getId())) {
                        throw new AccessDeniedToResourceException();
                    }

                    urlToSnapshot = original.getUrl();
                    inputImageId = original.getId();
                    break;
                default: continue;
            }

            String finalUrl = urlToSnapshot.startsWith("contents/") ? urlToSnapshot : gcsService.copyFile(urlToSnapshot, "contents");
            urlsToKeep.add(finalUrl);

            ContentImage newImg = ContentImage.builder().content(content).inputImageId(inputImageId).url(finalUrl).displayOrder(order++).build();
            contentImageRepository.save(newImg);
        }

        // 2. Identify URLs to delete (those that were present but are no longer used)
        Set<String> urlsToDelete = new HashSet<>(urlsBeforeUpdate);
        urlsToDelete.removeAll(urlsToKeep);

        // 3. Register GCS cleanup after transaction commit
        if (!urlsToDelete.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Transaction committed. Cleaning up {} unused GCS files for content {}", urlsToDelete.size(), contentId);
                    urlsToDelete.forEach(url -> {
                        try {
                            gcsService.deleteFile(url);
                        } catch (Exception e) {
                            log.error("Failed to delete GCS file after commit: {}", url);
                        }
                    });
                }
            });
        }

        List<ContentImage> updatedImages = contentImageRepository.findByContentOrderByDisplayOrderAsc(content);
        boolean isPosted = contentPostRepository.existsByContentAndStatus(content, PublishStatus.COMPLETED);
        return ContentResponse.from(content, updatedImages, isPosted, gcsService::generateSignedUrl);
    }

    @Transactional
    public GenerateContentResponse startRegeneration(User user, UUID contentId, RegenerateContentRequest request) {
        Content originalContent = contentRepository.findByIdAndIsDeletedFalse(contentId)
                .orElseThrow(ContentNotFoundException::new);

        if (!originalContent.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        TargetPlatform.from(request.getTarget());
        GenerationRequest genRequest = originalContent.getGenerationRequest();

        ApiLog apiLog = ApiLog.builder()
                .generationRequest(genRequest)
                .model(geminiConfig.getModel())
                .status(ApiStatus.PROCESSING)
                .build();

        ApiLog savedLog = apiLogRepository.save(apiLog);

        regenerateContentAsync(genRequest.getId(), originalContent.getId(), request, savedLog.getId());

        return GenerateContentResponse.builder()
                .requestId(genRequest.getId())
                .taskId(savedLog.getId())
                .startedAt(LocalDateTime.now())
                .build();
    }

    @Async
    public void regenerateContentAsync(UUID requestId, UUID originalContentId, RegenerateContentRequest regenerateRequest, UUID apiLogId) {
        log.info("Starting background regeneration for request: {}, apiLog: {}", requestId, apiLogId);

        Mono.fromCallable(() -> {
                    return transactionTemplate.execute(status -> {
                        Content originalContent = contentRepository.findById(originalContentId).orElseThrow();

                        String originalText;
                        List<String> originalHashtags;
                        TargetPlatform targetPlatform = TargetPlatform.from(regenerateRequest.getTarget());
                        if (targetPlatform == TargetPlatform.INSTAGRAM) {
                            originalText = originalContent.getInstagramText();
                            originalHashtags = originalContent.getInstagramHashtags();
                        } else if (targetPlatform == TargetPlatform.KARROT) {
                            originalText = originalContent.getKarrotText();
                            originalHashtags = originalContent.getKarrotTags();
                        } else {
                            originalText = originalContent.getNaverText();
                            originalHashtags = originalContent.getNaverKeywords();
                        }

                        return GeminiRegenerationRequest.builder()
                                .originalText(originalText)
                                .originalHashtags(originalHashtags)
                                .feedback(regenerateRequest.getFeedback())
                                .target(targetPlatform.name())
                                .build();
                    });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(geminiService::generateProcessedRegenerationContent)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(parsed -> {
                    transactionTemplate.execute(status -> {
                        ApiLog apiLog = apiLogRepository.findById(apiLogId).orElseThrow();
                        GenerationRequest request = requestRepository.findById(requestId).orElseThrow();
                        Content originalContent = contentRepository.findById(originalContentId).orElseThrow();

                        // Merge with original content (Only update target platform)
                        TargetPlatform targetPlatform = TargetPlatform.from(regenerateRequest.getTarget());
                        Content newContent = Content.builder()
                                .generationRequest(request)
                                .instagramText(targetPlatform == TargetPlatform.INSTAGRAM && parsed.getText() != null ? parsed.getText() : originalContent.getInstagramText())
                                .instagramHashtags(targetPlatform == TargetPlatform.INSTAGRAM && parsed.getHashtags() != null ? parsed.getHashtags() : originalContent.getInstagramHashtags())
                                .karrotText(targetPlatform == TargetPlatform.KARROT && parsed.getText() != null ? parsed.getText() : originalContent.getKarrotText())
                                .karrotTags(targetPlatform == TargetPlatform.KARROT && parsed.getHashtags() != null ? parsed.getHashtags() : originalContent.getKarrotTags())
                                .naverText(targetPlatform == TargetPlatform.NAVER && parsed.getText() != null ? parsed.getText() : originalContent.getNaverText())
                                .naverKeywords(targetPlatform == TargetPlatform.NAVER && parsed.getHashtags() != null ? parsed.getHashtags() : originalContent.getNaverKeywords())
                                .generationType(GenerationType.TEXT_ONLY)
                                .aiModel(apiLog.getModel())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        Content savedContent = contentRepository.save(newContent);

                        // Reuse images from original content
                        // TODO: regenerateImage=true가 아직 미구현 상태
                        List<ContentImage> originalImages = contentImageRepository.findByContentOrderByCreatedAtAsc(originalContent);
                        for (ContentImage originalImg : originalImages) {
                            ContentImage newImg = ContentImage.builder()
                                    .content(savedContent)
                                    .inputImageId(originalImg.getInputImageId())
                                    .url(originalImg.getUrl())
                                    .build();
                            contentImageRepository.save(newImg);
                        }

                        // Update ApiLog with Metadata
                        Integer inputTokens = parsed.getInputTokens();
                        Integer outputTokens = parsed.getOutputTokens();
                        BigDecimal costUsd = calculateCost(inputTokens, outputTokens);
                        Integer responseTimeMs = parsed.getResponseTimeMs() != null ? parsed.getResponseTimeMs().intValue() : null;

                        apiLog.completeSuccess(savedContent, inputTokens, outputTokens, costUsd, responseTimeMs);
                        apiLogRepository.save(apiLog);
                        return null;
                    });
                })
                .doOnError(e -> {
                    String safeErrorMessage = toSafeTaskErrorMessage(e);
                    log.error("Error during background regeneration for apiLog {} [{}]: {}",
                            apiLogId,
                            Exceptions.unwrap(e).getClass().getSimpleName(),
                            safeErrorMessage);
                    transactionTemplate.execute(status -> {
                        ApiLog apiLog = apiLogRepository.findById(apiLogId).orElseThrow();
                        apiLog.completeError(safeErrorMessage);
                        apiLogRepository.save(apiLog);
                        return null;
                    });
                })
                .subscribe();
    }

    String toSafeTaskErrorMessage(Throwable throwable) {
        Throwable cause = Exceptions.unwrap(throwable);
        if (cause instanceof CustomException customException) {
            return customException.getErrorCode().getMessage();
        }
        return ErrorCode.AI_REQUEST_FAILED.getMessage();
    }

    private BigDecimal calculateCost(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null || outputTokens == null) return null;

        // Cost calculation ($0.50 per 1M input, $3.00 per 1M output)
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .multiply(new BigDecimal("0.50"))
                .divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .multiply(new BigDecimal("3.00"))
                .divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP);
        return inputCost.add(outputCost);
    }

    @Transactional
    public GenerateContentResponse initiateVariation(User user, UUID inputImageId) {
        InputImage inputImage = inputImageRepository.findById(inputImageId).orElseThrow(
                () -> new RuntimeException("Input image not found"));

        if (!inputImage.getGenerationRequest().getUser().getId().equals(user.getId()))
            throw new AccessDeniedToResourceException();

        ApiLog apiLog = ApiLog.builder()
                .generationRequest(inputImage.getGenerationRequest())
                .model("COMFY_UI_VARIATION_V1")
                .status(ApiStatus.PROCESSING)
                .build();

        ApiLog savedLog = apiLogRepository.save(apiLog);
        String signedUrl = gcsService.generateSignedUrl(inputImage.getUrl());
        runComfyService.requestVariation(signedUrl, savedLog.getId().toString(), inputImageId.toString())
                .subscribe(response -> log.info("Successfully initiated RunComfy variation: {}", response.getRequestId()), error -> {
                    log.error("Failed to initiate RunComfy variation for apiLogId: {}", savedLog.getId());
                    transactionTemplate.execute(status -> {
                        ApiLog logToUpdate = apiLogRepository.findById(savedLog.getId()).orElseThrow();
                        logToUpdate.completeError("Failed to initiate: " + error.getMessage());
                        return apiLogRepository.save(logToUpdate);
                    });
                });

        return GenerateContentResponse.builder().requestId(inputImage.getGenerationRequest().getId()).taskId(savedLog.getId()).startedAt(LocalDateTime.now()).build();
    }

    @Transactional(readOnly = true)
    public List<InputImageVariationResponse> getImageVariations(User user, UUID inputImageId) {
        InputImage inputImage = inputImageRepository.findById(inputImageId).orElseThrow(() -> new RuntimeException("Input image not found"));
        if (!inputImage.getGenerationRequest().getUser().getId().equals(user.getId())) throw new AccessDeniedToResourceException();
        return inputImageVariationRepository.findByInputImageOrderByCreatedAtAsc(inputImage).stream().map(v -> InputImageVariationResponse.from(v, gcsService::generateSignedUrl)).collect(Collectors.toList());
    }
}