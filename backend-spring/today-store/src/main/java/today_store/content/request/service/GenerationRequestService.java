package today_store.content.request.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import today_store.authentication.entity.User;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.authentication.exception.UserNotFoundException;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.content.request.dto.*;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;
import today_store.content.request.exception.FileNameMismatchException;
import today_store.content.request.exception.GenerationRequestNotFoundException;
import today_store.content.request.exception.InvalidRequestBodyFormatException;
import today_store.content.request.repository.GenerationRequestRepository;
import today_store.content.request.repository.InputImageRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationRequestService {

    private final GenerationRequestRepository requestRepository;
    private final InputImageRepository imageRepository;
    private final UserRepository userRepository;
    private final GcsService gcsService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateGenerationResponse createRequest(User user, CreateGenerationRequest request) {
        log.info("Creating generation request for user ID: {}, concept: {}", user.getId(), request.getConcept());

        if (request.getImages().size() != request.getImageDescriptions().size()) {
            log.warn("Mismatched image files and descriptions for user ID: {}", user.getId());
            throw new CustomException(ErrorCode.GENERATION_REQUEST_REQUIRED_FIELDS_MISSING);
        }

        GenerationRequest generationRequest = GenerationRequest.builder()
                .user(user)
                .concept(request.getConcept())
                .additionalNote(request.getAdditionalNote())
                .targetAge(request.getTargetAge())
                .targetGender(request.getTargetGender())
                .createdAt(LocalDateTime.now())
                .build();

        GenerationRequest savedRequest = requestRepository.save(generationRequest);
        log.debug("Saved generation request entity. ID: {}", savedRequest.getId());

        for (int i = 0; i < request.getImages().size(); i++) {
            MultipartFile imageFile = request.getImages().get(i);
            String description = request.getImageDescriptions().get(i);

            log.debug("Uploading image {}/{} for request ID: {}", (i + 1), request.getImages().size(), savedRequest.getId());
            String url = gcsService.uploadFile(imageFile, "requests");

            InputImage inputImage = InputImage.builder()
                    .generationRequest(savedRequest)
                    .url(url)
                    .description(description)
                    .displayOrder(i + 1)
                    .build();
            imageRepository.save(inputImage);
        }

        log.info("Generation request created successfully. ID: {}, User: {}", savedRequest.getId(), user.getId());
        return CreateGenerationResponse.from(savedRequest);
    }

    @Transactional(readOnly = true)
    public GenerationRequestListResponse getRequests(User user, Pageable pageable) {
        log.debug("Fetching requests for user: {}, pageable: {}", user.getId(), pageable);
        Page<GenerationRequest> requestPage = requestRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user, pageable);

        List<GenerationRequestListResponse.GenerationRequestSummary> data = requestPage.getContent().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        GenerationRequestListResponse.PaginationInfo pagination = GenerationRequestListResponse.PaginationInfo.from(requestPage);

        return GenerationRequestListResponse.from(data, pagination);
    }

    private GenerationRequestListResponse.GenerationRequestSummary toSummary(GenerationRequest request) {
        List<InputImage> images = imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(request);
        String thumbnailUrl = images.isEmpty() ? null : gcsService.generateSignedUrl(images.get(0).getUrl());

        return GenerationRequestListResponse.GenerationRequestSummary.from(request, thumbnailUrl, images.size());
    }

    @Transactional(readOnly = true)
    public GenerationRequestDetailResponse getRequestDetail(User user, UUID requestId) {
        log.debug("Fetching request detail. ID: {}, User: {}", requestId, user.getId());
        GenerationRequest request = requestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> {
                    log.warn("Generation request not found: {}", requestId);
                    return new GenerationRequestNotFoundException();
                });

        if (!request.getUser().getId().equals(user.getId())) {
            log.warn("Access denied: User {} attempted to access request {} owned by {}",
                    user.getId(), requestId, request.getUser().getId());
            throw new AccessDeniedToResourceException();
        }

        List<InputImage> images = imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(request);
        List<GenerationRequestDetailResponse.ImageResponse> imageResponses = images.stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());

        return GenerationRequestDetailResponse.from(request, imageResponses);
    }

    private GenerationRequestDetailResponse.ImageResponse toImageResponse(InputImage image) {
        return GenerationRequestDetailResponse.ImageResponse.from(image, gcsService.generateSignedUrl(image.getUrl()));
    }

    @Transactional
    public void deleteRequest(User user, UUID requestId) {
        log.info("Delete request for ID: {} by user: {}", requestId, user.getId());
        GenerationRequest request = requestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> {
                    log.warn("Generation request not found for deletion: {}", requestId);
                    return new GenerationRequestNotFoundException();
                });

        if (!request.getUser().getId().equals(user.getId())) {
            log.warn("Access denied for deletion: User {} attempted to delete request {} owned by {}",
                    user.getId(), requestId, request.getUser().getId());
            throw new AccessDeniedToResourceException();
        }

        // soft delete
        request.delete();
        log.info("Generation request soft-deleted successfully. ID: {}", requestId);
    }

    @Transactional
    public UpdateGenerationResponse updateRequest(String email, UUID requestId, UpdateGenerationRequest request) {
        String maskedEmail = email.replaceAll("(?<=.{3}).(?=.*@)", "*");
        log.info("Update requested for ID: {} by user: {}", requestId, maskedEmail);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found during request update: {}", maskedEmail);
                    return new UserNotFoundException();
                });

        // 1. parse imageConfigs
        List<ImageConfig> configs = new ArrayList<>();
        if (request.getImageConfigs() != null) {
            try {
                configs = objectMapper.readValue(request.getImageConfigs(), new TypeReference<List<ImageConfig>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("imageConfigs JSON parsing failed for user: {}, requestId: {}", user.getId(), requestId);
                throw new InvalidRequestBodyFormatException();
            }
        }

        GenerationRequest generationRequest = requestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> {
                    log.warn("Generation request not found for update: {}", requestId);
                    return new GenerationRequestNotFoundException();
                });

        if (!generationRequest.getUser().getId().equals(user.getId())) {
            log.warn("Access denied for update: User {} attempted to update request {} owned by {}",
                    user.getId(), requestId, generationRequest.getUser().getId());
            throw new AccessDeniedToResourceException();
        }

        generationRequest.update(request.getConcept(), request.getAdditionalNote(), request.getTargetAge(), request.getTargetGender());

        // 2. 상태 동기화 (삭제 로직 자동화)
        List<InputImage> currentImages = imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest);
        Set<UUID> keepIds = configs.stream()
                .map(ImageConfig::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> urlsToDelete = new ArrayList<>();
        int deletedCount = 0;
        for (InputImage img : currentImages) {
            if (!keepIds.contains(img.getId())) {
                log.debug("Deleting image {} as it's not in the update configuration (State Sync)", img.getId());
                urlsToDelete.add(img.getUrl());
                imageRepository.delete(img);
                deletedCount++;
            }
        }

        // 3. 수정 및 신규 추가 처리
        int addedCount = 0;
        if (!configs.isEmpty()) {
            Map<String, MultipartFile> fileMap = new HashMap<>();
            if (request.getNewImages() != null) {
                for (MultipartFile file : request.getNewImages()) {
                    fileMap.put(file.getOriginalFilename(), file);
                }
            }

            for (ImageConfig config : configs) {
                if (config.getId() != null) {
                    // 기존 이미지 정보 업데이트
                    InputImage img = imageRepository.findById(config.getId())
                            .orElseThrow(GenerationRequestNotFoundException::new);

                    if (!img.getGenerationRequest().getId().equals(requestId)) {
                        log.warn("Security alert: Attempt to update image {} belonging to request {}", config.getId(), img.getGenerationRequest().getId());
                        throw new AccessDeniedToResourceException();
                    }

                    img.update(config.getDescription(), config.getDisplayOrder());
                } else {
                    // 신규 이미지 추가
                    MultipartFile file = fileMap.get(config.getFileName());
                    if (file == null) {
                        log.warn("File name mismatch during update: {}", config.getFileName());
                        throw new FileNameMismatchException();
                    }
                    String url = gcsService.uploadFile(file, "requests");
                    InputImage newImg = InputImage.builder()
                            .generationRequest(generationRequest)
                            .url(url)
                            .description(config.getDescription())
                            .displayOrder(config.getDisplayOrder())
                            .build();
                    imageRepository.save(newImg);
                    addedCount++;
                }
            }
        }

        // 5. 트랜잭션 커밋 성공 후 GCS 실제 삭제를 위한 동기화 등록
        if (!urlsToDelete.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("Transaction committed. Starting GCS file cleanup for {} files.",
                            urlsToDelete.size());
                    urlsToDelete.forEach(url -> {
                        try {
                            gcsService.deleteFile(url);
                        } catch (Exception e) {
                            log.error("Failed to delete GCS file after commit: {}", url, e);
                        }
                    });
                }
            });
        }


        requestRepository.saveAndFlush(generationRequest);

        List<InputImage> finalImages = imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest);
        UpdateGenerationResponse.ImageSummary imageSummary = UpdateGenerationResponse.ImageSummary.from(finalImages.size(), addedCount, deletedCount);

        log.info("Update completed for request {}. Added: {}, Deleted: {}, Final Count: {}",
                requestId, addedCount, deletedCount, finalImages.size());



        return UpdateGenerationResponse.from(generationRequest, imageSummary);
    }
}
