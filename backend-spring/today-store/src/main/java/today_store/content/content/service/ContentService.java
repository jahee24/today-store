package today_store.content.content.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import today_store.authentication.entity.User;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.authentication.repository.UserRepository;
import today_store.common.config.GeminiConfig;
import today_store.common.gcs.GcsService;
import today_store.common.gemini.dto.GeminiPromptRequest;
import today_store.common.gemini.dto.GeminiRegenerationRequest;
import today_store.common.gemini.service.GeminiService;
import today_store.content.content.dto.*;
import today_store.content.content.entity.*;
import today_store.content.content.repository.ApiLogRepository;
import today_store.content.content.repository.ContentImageRepository;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.repository.ContentRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ContentPostRepository contentPostRepository;
    private final GeminiService geminiService;
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

        // Create ApiLog for tracking
        ApiLog apiLog = ApiLog.builder()
                .generationRequest(request)
                .model(geminiConfig.getModel())
                .status(ApiStatus.PROCESSING)
                .build();

        ApiLog savedLog = apiLogRepository.save(apiLog);

        // Start generation in background
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
                    log.error("Error during background generation for apiLog {}: {}", apiLogId, e.getMessage(), e);
                    transactionTemplate.execute(status -> {
                        ApiLog apiLog = apiLogRepository.findById(apiLogId).orElseThrow();
                        apiLog.completeError(e.getMessage());
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
    public GenerateContentResponse startRegeneration(User user, UUID contentId, RegenerateContentRequest request) {
        Content originalContent = contentRepository.findByIdAndIsDeletedFalse(contentId)
                .orElseThrow(ContentNotFoundException::new);

        if (!originalContent.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

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
                        String target = regenerateRequest.getTarget();
                        if ("INSTAGRAM".equals(target)) {
                            originalText = originalContent.getInstagramText();
                            originalHashtags = originalContent.getInstagramHashtags();
                        } else if ("KARROT".equals(target)) {
                            originalText = originalContent.getKarrotText();
                            originalHashtags = originalContent.getKarrotTags();
                        } else if ("NAVER".equals(target)) {
                            originalText = originalContent.getNaverText();
                            originalHashtags = originalContent.getNaverKeywords();
                        } else {
                            originalText = "";
                            originalHashtags = List.of();
                        }

                        return GeminiRegenerationRequest.builder()
                                .originalText(originalText)
                                .originalHashtags(originalHashtags)
                                .feedback(regenerateRequest.getFeedback())
                                .target(target)
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
                        String target = regenerateRequest.getTarget();
                        Content newContent = Content.builder()
                                .generationRequest(request)
                                .instagramText("INSTAGRAM".equals(target) && parsed.getText() != null ? parsed.getText() : originalContent.getInstagramText())
                                .instagramHashtags("INSTAGRAM".equals(target) && parsed.getHashtags() != null ? parsed.getHashtags() : originalContent.getInstagramHashtags())
                                .karrotText("KARROT".equals(target) && parsed.getText() != null ? parsed.getText() : originalContent.getKarrotText())
                                .karrotTags("KARROT".equals(target) && parsed.getHashtags() != null ? parsed.getHashtags() : originalContent.getKarrotTags())
                                .naverText("NAVER".equals(target) && parsed.getText() != null ? parsed.getText() : originalContent.getNaverText())
                                .naverKeywords("NAVER".equals(target) && parsed.getHashtags() != null ? parsed.getHashtags() : originalContent.getNaverKeywords())
                                .generationType(GenerationType.TEXT_ONLY)
                                .aiModel(apiLog.getModel())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        Content savedContent = contentRepository.save(newContent);

                        // Reuse images from original content
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
                    log.error("Error during background regeneration for apiLog {}: {}", apiLogId, e.getMessage(), e);
                    transactionTemplate.execute(status -> {
                        ApiLog apiLog = apiLogRepository.findById(apiLogId).orElseThrow();
                        apiLog.completeError(e.getMessage());
                        apiLogRepository.save(apiLog);
                        return null;
                    });
                })
                .subscribe();
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
}