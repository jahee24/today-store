package today_store.content.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import today_store.authentication.entity.User;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.authentication.exception.UserNotFoundException;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gcs.GcsService;
import today_store.content.request.dto.CreateGenerationRequest;
import today_store.content.request.dto.CreateGenerationResponse;
import today_store.content.request.dto.GenerationRequestDetailResponse;
import today_store.content.request.dto.GenerationRequestListResponse;
import today_store.content.request.dto.ImageConfig;
import today_store.content.request.dto.UpdateGenerationRequest;
import today_store.content.request.dto.UpdateGenerationResponse;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;
import today_store.content.request.exception.FileNameMismatchException;
import today_store.content.request.exception.GenerationRequestNotFoundException;
import today_store.content.request.exception.InvalidRequestBodyFormatException;
import today_store.content.request.repository.GenerationRequestRepository;
import today_store.content.request.repository.InputImageRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("생성 요청 서비스 테스트")
class GenerationRequestServiceTest {

    @Mock
    private GenerationRequestRepository requestRepository;

    @Mock
    private InputImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GcsService gcsService;

    @Mock
    private ObjectMapper objectMapper;

    private GenerationRequestService generationRequestService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        generationRequestService = new GenerationRequestService(
                requestRepository,
                imageRepository,
                userRepository,
                gcsService,
                objectMapper
        );
    }

    @Test
    @DisplayName("생성 요청 생성 성공")
    void shouldCreateGenerationRequest() {
        // 이미지와 설명 수가 일치하면 요청과 입력 이미지를 저장하고 생성 응답을 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 16, 0);
        MockMultipartFile firstImage = createMultipartFile("images", "first.png", "first-image");
        MockMultipartFile secondImage = createMultipartFile("images", "second.png", "second-image");
        CreateGenerationRequest request = CreateGenerationRequest.builder()
                .concept("봄 프로모션")
                .additionalNote("따뜻한 분위기")
                .targetAge("20대")
                .targetGender("여성")
                .imageDescriptions(List.of("정면 이미지", "측면 이미지"))
                .images(List.of(firstImage, secondImage))
                .build();
        given(requestRepository.save(any(GenerationRequest.class))).willAnswer(invocation -> {
            GenerationRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", requestId);
            ReflectionTestUtils.setField(saved, "createdAt", createdAt);
            return saved;
        });
        given(gcsService.uploadFile(firstImage, "requests")).willReturn("2026/03/30/requests/uuid_first.png");
        given(gcsService.uploadFile(secondImage, "requests")).willReturn("2026/03/30/requests/uuid_second.png");

        // when
        CreateGenerationResponse response = generationRequestService.createRequest(user, request);

        // then
        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        then(requestRepository).should().save(argThat(saved ->
                saved.getUser() == user
                        && saved.getConcept().equals("봄 프로모션")
                        && saved.getAdditionalNote().equals("따뜻한 분위기")
                        && saved.getTargetAge().equals("20대")
                        && saved.getTargetGender().equals("여성")
        ));
        then(gcsService).should().uploadFile(firstImage, "requests");
        then(gcsService).should().uploadFile(secondImage, "requests");

        ArgumentCaptor<InputImage> imageCaptor = ArgumentCaptor.forClass(InputImage.class);
        then(imageRepository).should(times(2)).save(imageCaptor.capture());
        assertThat(imageCaptor.getAllValues()).hasSize(2);
        assertThat(imageCaptor.getAllValues().get(0).getDescription()).isEqualTo("정면 이미지");
        assertThat(imageCaptor.getAllValues().get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(imageCaptor.getAllValues().get(1).getDescription()).isEqualTo("측면 이미지");
        assertThat(imageCaptor.getAllValues().get(1).getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("생성 요청 생성 필수값 불일치")
    void shouldThrowWhenImageCountAndDescriptionCountDoNotMatch() {
        // 이미지 수와 설명 수가 다르면 C001 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        CreateGenerationRequest request = CreateGenerationRequest.builder()
                .concept("봄 프로모션")
                .imageDescriptions(List.of("정면 이미지"))
                .images(List.of(
                        createMultipartFile("images", "first.png", "first-image"),
                        createMultipartFile("images", "second.png", "second-image")
                ))
                .build();

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> generationRequestService.createRequest(user, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GENERATION_REQUEST_REQUIRED_FIELDS_MISSING);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.GENERATION_REQUEST_REQUIRED_FIELDS_MISSING.getMessage());
        then(requestRepository).should(never()).save(any(GenerationRequest.class));
    }

    @Test
    @DisplayName("생성 요청 목록 조회 성공")
    void shouldReturnGenerationRequestList() {
        // 삭제되지 않은 요청 목록을 썸네일과 페이징 정보로 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        GenerationRequest generationRequest = createGenerationRequest(
                user,
                UUID.randomUUID(),
                "봄 프로모션",
                "추가 메모",
                "20대",
                "여성",
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        Page<GenerationRequest> page = new PageImpl<>(List.of(generationRequest), pageable, 1);
        InputImage thumbnail = createInputImage(
                generationRequest,
                UUID.randomUUID(),
                "stored-thumbnail",
                "대표 이미지",
                1,
                LocalDateTime.of(2026, 3, 30, 12, 5)
        );
        given(requestRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user, pageable)).willReturn(page);
        given(imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest)).willReturn(List.of(thumbnail));
        given(gcsService.generateSignedUrl("stored-thumbnail")).willReturn("https://signed.example.com/thumbnail");

        // when
        GenerationRequestListResponse response = generationRequestService.getRequests(user, pageable);

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getRequestId()).isEqualTo(generationRequest.getId());
        assertThat(response.getData().get(0).getConcept()).isEqualTo("봄 프로모션");
        assertThat(response.getData().get(0).getThumbnailUrl()).isEqualTo("https://signed.example.com/thumbnail");
        assertThat(response.getData().get(0).getImageCount()).isEqualTo(1);
        assertThat(response.getPagination().getCurrentPage()).isEqualTo(1);
        assertThat(response.getPagination().getPageSize()).isEqualTo(10);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1);
        assertThat(response.getPagination().getTotalPages()).isEqualTo(1);
        assertThat(response.getPagination().getHasNext()).isFalse();
        assertThat(response.getPagination().getHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("생성 요청 상세 조회 성공")
    void shouldReturnGenerationRequestDetail() {
        // 본인 소유 요청의 상세 정보와 signed URL 이미지를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        GenerationRequest generationRequest = createGenerationRequest(
                user,
                requestId,
                "봄 프로모션",
                "추가 메모",
                "20대",
                "여성",
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        InputImage firstImage = createInputImage(
                generationRequest,
                UUID.randomUUID(),
                "stored-first",
                "정면 이미지",
                1,
                LocalDateTime.of(2026, 3, 30, 12, 5)
        );
        InputImage secondImage = createInputImage(
                generationRequest,
                UUID.randomUUID(),
                "stored-second",
                "측면 이미지",
                2,
                LocalDateTime.of(2026, 3, 30, 12, 6)
        );
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));
        given(imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest)).willReturn(List.of(firstImage, secondImage));
        given(gcsService.generateSignedUrl("stored-first")).willReturn("https://signed.example.com/first");
        given(gcsService.generateSignedUrl("stored-second")).willReturn("https://signed.example.com/second");

        // when
        GenerationRequestDetailResponse response = generationRequestService.getRequestDetail(user, requestId);

        // then
        assertThat(response.getId()).isEqualTo(requestId);
        assertThat(response.getUserId()).isEqualTo(user.getId());
        assertThat(response.getConcept()).isEqualTo("봄 프로모션");
        assertThat(response.getAdditionalNote()).isEqualTo("추가 메모");
        assertThat(response.getImages()).hasSize(2);
        assertThat(response.getImages().get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(response.getImages().get(0).getUrl()).isEqualTo("https://signed.example.com/first");
        assertThat(response.getImages().get(1).getDisplayOrder()).isEqualTo(2);
        assertThat(response.getImages().get(1).getUrl()).isEqualTo("https://signed.example.com/second");
    }

    @Test
    @DisplayName("존재하지 않는 생성 요청 상세 조회 실패")
    void shouldThrowWhenRequestDetailDoesNotExist() {
        // 존재하지 않는 요청 상세를 조회하면 C005 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.empty());

        // when
        GenerationRequestNotFoundException exception = assertThrows(
                GenerationRequestNotFoundException.class,
                () -> generationRequestService.getRequestDetail(user, requestId)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("타 사용자 생성 요청 상세 조회 거부")
    void shouldThrowWhenAccessingAnotherUsersRequestDetail() {
        // 다른 사용자의 요청을 조회하면 A008 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        User anotherUser = createUser("other@example.com");
        UUID requestId = UUID.randomUUID();
        GenerationRequest generationRequest = createGenerationRequest(
                anotherUser,
                requestId,
                "봄 프로모션",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> generationRequestService.getRequestDetail(user, requestId)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE.getMessage());
    }

    @Test
    @DisplayName("생성 요청 삭제 성공")
    void shouldSoftDeleteGenerationRequest() {
        // 본인 소유 요청을 삭제하면 soft delete 처리가 되어야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        GenerationRequest generationRequest = createGenerationRequest(
                user,
                requestId,
                "봄 프로모션",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));

        // when
        generationRequestService.deleteRequest(user, requestId);

        // then
        assertThat(ReflectionTestUtils.getField(generationRequest, "isDeleted")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(generationRequest, "deletedAt")).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 생성 요청 삭제 실패")
    void shouldThrowWhenDeletingMissingRequest() {
        // 존재하지 않는 요청을 삭제하면 C005 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.empty());

        // when
        GenerationRequestNotFoundException exception = assertThrows(
                GenerationRequestNotFoundException.class,
                () -> generationRequestService.deleteRequest(user, requestId)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("타 사용자 생성 요청 삭제 거부")
    void shouldThrowWhenDeletingAnotherUsersRequest() {
        // 다른 사용자의 요청을 삭제하면 A008 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        User anotherUser = createUser("other@example.com");
        UUID requestId = UUID.randomUUID();
        GenerationRequest generationRequest = createGenerationRequest(
                anotherUser,
                requestId,
                "봄 프로모션",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> generationRequestService.deleteRequest(user, requestId)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE.getMessage());
    }

    @Test
    @DisplayName("생성 요청 수정 성공")
    void shouldUpdateGenerationRequest() throws Exception {
        // imageConfigs 기반 상태 동기화로 이미지 수정, 추가, 삭제와 응답 요약을 처리해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UUID keepImageId = UUID.randomUUID();
        UUID deletedImageId = UUID.randomUUID();
        UUID addedImageId = UUID.randomUUID();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 17, 0);
        GenerationRequest generationRequest = createGenerationRequest(
                user,
                requestId,
                "기존 컨셉",
                "기존 메모",
                "10대",
                "남성",
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        InputImage keepImage = createInputImage(
                generationRequest,
                keepImageId,
                "stored-keep",
                "기존 설명",
                1,
                LocalDateTime.of(2026, 3, 30, 12, 5)
        );
        InputImage deletedImage = createInputImage(
                generationRequest,
                deletedImageId,
                "stored-delete",
                "삭제될 이미지",
                2,
                LocalDateTime.of(2026, 3, 30, 12, 6)
        );
        InputImage addedImage = createInputImage(
                generationRequest,
                addedImageId,
                "stored-added",
                "신규 이미지",
                3,
                LocalDateTime.of(2026, 3, 30, 17, 1)
        );
        String imageConfigs = """
                [
                  {"id":"%s","description":"수정된 설명","displayOrder":2},
                  {"fileName":"new-image.png","description":"신규 이미지","displayOrder":3}
                ]
                """.formatted(keepImageId);
        MockMultipartFile newImageFile = createMultipartFile("newImages", "new-image.png", "new-image");
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .concept("수정된 컨셉")
                .additionalNote("수정된 메모")
                .targetAge("20대")
                .targetGender("여성")
                .imageConfigs(imageConfigs)
                .newImages(List.of(newImageFile))
                .build();
        List<ImageConfig> configs = List.of(
                ImageConfig.builder()
                        .id(keepImageId)
                        .description("수정된 설명")
                        .displayOrder(2)
                        .build(),
                ImageConfig.builder()
                        .fileName("new-image.png")
                        .description("신규 이미지")
                        .displayOrder(3)
                        .build()
        );
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(objectMapper.readValue(eq(imageConfigs), org.mockito.ArgumentMatchers.<TypeReference<List<ImageConfig>>>any()))
                .willReturn(configs);
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));
        given(imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest))
                .willReturn(List.of(keepImage, deletedImage), List.of(keepImage, addedImage));
        given(imageRepository.findById(keepImageId)).willReturn(Optional.of(keepImage));
        given(gcsService.uploadFile(newImageFile, "requests")).willReturn("stored-added");
        given(imageRepository.save(any(InputImage.class))).willReturn(addedImage);
        given(requestRepository.saveAndFlush(generationRequest)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(generationRequest, "updatedAt", updatedAt);
            return generationRequest;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            // when
            UpdateGenerationResponse response = generationRequestService.updateRequest("user@example.com", requestId, request);
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

            // then
            assertThat(response.getRequestId()).isEqualTo(requestId);
            assertThat(response.getConcept()).isEqualTo("수정된 컨셉");
            assertThat(response.getAdditionalNote()).isEqualTo("수정된 메모");
            assertThat(response.getTargetAge()).isEqualTo("20대");
            assertThat(response.getTargetGender()).isEqualTo("여성");
            assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(response.getImageSummary().getTotalCount()).isEqualTo(2);
            assertThat(response.getImageSummary().getAddedCount()).isEqualTo(1);
            assertThat(response.getImageSummary().getDeletedCount()).isEqualTo(1);
            assertThat(generationRequest.getConcept()).isEqualTo("수정된 컨셉");
            assertThat(keepImage.getDescription()).isEqualTo("수정된 설명");
            assertThat(keepImage.getDisplayOrder()).isEqualTo(2);
            then(imageRepository).should().delete(deletedImage);
            then(requestRepository).should().saveAndFlush(generationRequest);
            then(gcsService).should(never()).deleteFile("stored-delete");
            assertThat(synchronizations).hasSize(1);

            synchronizations.forEach(TransactionSynchronization::afterCommit);

            then(gcsService).should().deleteFile("stored-delete");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("존재하지 않는 사용자 생성 요청 수정 실패")
    void shouldThrowWhenUpdatingRequestForMissingUser() {
        // 수정 요청 이메일에 해당하는 사용자가 없으면 A007 예외를 반환해야 한다.

        // given
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .imageConfigs("[]")
                .build();
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> generationRequestService.updateRequest("missing@example.com", UUID.randomUUID(), request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("생성 요청 수정 JSON 형식 오류")
    void shouldThrowWhenImageConfigsJsonIsInvalid() throws Exception {
        // imageConfigs JSON 파싱에 실패하면 C003 예외를 반환해야 한다.

        // given
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .imageConfigs("invalid-json")
                .build();
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(createUser("user@example.com")));
        given(objectMapper.readValue(eq("invalid-json"), org.mockito.ArgumentMatchers.<TypeReference<List<ImageConfig>>>any()))
                .willThrow(new JsonProcessingException("invalid") {
                });

        // when
        InvalidRequestBodyFormatException exception = assertThrows(
                InvalidRequestBodyFormatException.class,
                () -> generationRequestService.updateRequest("user@example.com", UUID.randomUUID(), request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_BODY_FORMAT);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_REQUEST_BODY_FORMAT.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 생성 요청 수정 실패")
    void shouldThrowWhenUpdatingMissingRequest() throws Exception {
        // 수정 대상 요청이 없으면 C005 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .imageConfigs("[]")
                .build();
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(objectMapper.readValue(eq("[]"), org.mockito.ArgumentMatchers.<TypeReference<List<ImageConfig>>>any()))
                .willReturn(List.of());
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.empty());

        // when
        GenerationRequestNotFoundException exception = assertThrows(
                GenerationRequestNotFoundException.class,
                () -> generationRequestService.updateRequest("user@example.com", requestId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("타 사용자 생성 요청 수정 거부")
    void shouldThrowWhenUpdatingAnotherUsersRequest() throws Exception {
        // 다른 사용자의 요청을 수정하면 A008 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        User anotherUser = createUser("other@example.com");
        UUID requestId = UUID.randomUUID();
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .imageConfigs("[]")
                .build();
        GenerationRequest generationRequest = createGenerationRequest(
                anotherUser,
                requestId,
                "기존 컨셉",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(objectMapper.readValue(eq("[]"), org.mockito.ArgumentMatchers.<TypeReference<List<ImageConfig>>>any()))
                .willReturn(List.of());
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> generationRequestService.updateRequest("user@example.com", requestId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE.getMessage());
    }

    @Test
    @DisplayName("다른 요청 소속 이미지 수정 거부")
    void shouldThrowWhenUpdatingImageBelongingToAnotherRequest() throws Exception {
        // 다른 요청에 속한 이미지 ID를 수정하려 하면 A008 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .imageConfigs("""
                        [{"id":"%s","description":"수정된 설명","displayOrder":1}]
                        """.formatted(imageId))
                .build();
        GenerationRequest generationRequest = createGenerationRequest(
                user,
                requestId,
                "기존 컨셉",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        GenerationRequest anotherRequest = createGenerationRequest(
                user,
                UUID.randomUUID(),
                "다른 요청",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 11, 0),
                LocalDateTime.of(2026, 3, 30, 11, 30)
        );
        InputImage foreignImage = createInputImage(
                anotherRequest,
                imageId,
                "stored-foreign",
                "다른 요청 이미지",
                1,
                LocalDateTime.of(2026, 3, 30, 11, 5)
        );
        List<ImageConfig> configs = List.of(
                ImageConfig.builder()
                        .id(imageId)
                        .description("수정된 설명")
                        .displayOrder(1)
                        .build()
        );
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(objectMapper.readValue(eq(request.getImageConfigs()), org.mockito.ArgumentMatchers.<TypeReference<List<ImageConfig>>>any()))
                .willReturn(configs);
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));
        given(imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest)).willReturn(List.of());
        given(imageRepository.findById(imageId)).willReturn(Optional.of(foreignImage));

        // when
        AccessDeniedToResourceException exception = assertThrows(
                AccessDeniedToResourceException.class,
                () -> generationRequestService.updateRequest("user@example.com", requestId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED_TO_RESOURCE.getMessage());
    }

    @Test
    @DisplayName("신규 이미지 파일명 불일치")
    void shouldThrowWhenNewImageFileNameDoesNotMatchConfig() throws Exception {
        // 신규 이미지 파일명이 imageConfigs와 맞지 않으면 C002 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .imageConfigs("""
                        [{"fileName":"expected.png","description":"신규 이미지","displayOrder":1}]
                        """)
                .newImages(List.of(createMultipartFile("newImages", "actual.png", "actual-image")))
                .build();
        GenerationRequest generationRequest = createGenerationRequest(
                user,
                requestId,
                "기존 컨셉",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        List<ImageConfig> configs = List.of(
                ImageConfig.builder()
                        .fileName("expected.png")
                        .description("신규 이미지")
                        .displayOrder(1)
                        .build()
        );
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(objectMapper.readValue(eq(request.getImageConfigs()), org.mockito.ArgumentMatchers.<TypeReference<List<ImageConfig>>>any()))
                .willReturn(configs);
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));
        given(imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest)).willReturn(List.of());

        // when
        FileNameMismatchException exception = assertThrows(
                FileNameMismatchException.class,
                () -> generationRequestService.updateRequest("user@example.com", requestId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_NAME_MISMATCH);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.FILE_NAME_MISMATCH.getMessage());
        then(gcsService).should(never()).uploadFile(any(), eq("requests"));
    }

    @Test
    @DisplayName("존재하지 않는 기존 이미지 수정 실패")
    void shouldThrowWhenUpdatingMissingExistingImage() throws Exception {
        // imageConfigs에 지정한 기존 이미지 ID가 없으면 C005 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com");
        UUID requestId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        UpdateGenerationRequest request = UpdateGenerationRequest.builder()
                .imageConfigs("""
                        [{"id":"%s","description":"수정된 설명","displayOrder":1}]
                        """.formatted(imageId))
                .build();
        GenerationRequest generationRequest = createGenerationRequest(
                user,
                requestId,
                "기존 컨셉",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 12, 0),
                LocalDateTime.of(2026, 3, 30, 12, 30)
        );
        List<ImageConfig> configs = List.of(
                ImageConfig.builder()
                        .id(imageId)
                        .description("수정된 설명")
                        .displayOrder(1)
                        .build()
        );
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(objectMapper.readValue(eq(request.getImageConfigs()), org.mockito.ArgumentMatchers.<TypeReference<List<ImageConfig>>>any()))
                .willReturn(configs);
        given(requestRepository.findByIdAndIsDeletedFalse(requestId)).willReturn(Optional.of(generationRequest));
        given(imageRepository.findByGenerationRequestOrderByDisplayOrderAsc(generationRequest)).willReturn(List.of());
        given(imageRepository.findById(imageId)).willReturn(Optional.empty());

        // when
        GenerationRequestNotFoundException exception = assertThrows(
                GenerationRequestNotFoundException.class,
                () -> generationRequestService.updateRequest("user@example.com", requestId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.GENERATION_REQUEST_NOT_FOUND.getMessage());
    }

    private User createUser(String email) {
        User user = new User(email, "테스트 사용자", "google", UUID.randomUUID().toString(), "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private GenerationRequest createGenerationRequest(
            User user,
            UUID requestId,
            String concept,
            String additionalNote,
            String targetAge,
            String targetGender,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        GenerationRequest request = GenerationRequest.builder()
                .user(user)
                .concept(concept)
                .additionalNote(additionalNote)
                .targetAge(targetAge)
                .targetGender(targetGender)
                .build();
        ReflectionTestUtils.setField(request, "id", requestId);
        ReflectionTestUtils.setField(request, "createdAt", createdAt);
        ReflectionTestUtils.setField(request, "updatedAt", updatedAt);
        return request;
    }

    private InputImage createInputImage(
            GenerationRequest generationRequest,
            UUID imageId,
            String url,
            String description,
            int displayOrder,
            LocalDateTime createdAt
    ) {
        InputImage image = InputImage.builder()
                .generationRequest(generationRequest)
                .url(url)
                .description(description)
                .displayOrder(displayOrder)
                .build();
        ReflectionTestUtils.setField(image, "id", imageId);
        ReflectionTestUtils.setField(image, "createdAt", createdAt);
        return image;
    }

    private MockMultipartFile createMultipartFile(String parameterName, String originalFilename, String content) {
        return new MockMultipartFile(parameterName, originalFilename, "image/png", content.getBytes());
    }
}
