package today_store.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import today_store.authentication.dto.LoginRequest;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.authentication.exception.InvalidRefreshTokenException;
import today_store.common.dto.PageRequest;
import today_store.common.dto.ErrorResponse;
import today_store.store.dto.CreateStoreRequest;
import today_store.store.exception.StoreAlreadyExistException;
import today_store.store.exception.StoreNotFoundException;
import today_store.user.dto.UpdateUserProfileRequest;
import today_store.content.request.dto.CreateGenerationRequest;
import today_store.content.request.dto.UpdateGenerationRequest;
import today_store.content.request.exception.FileNameMismatchException;
import today_store.content.request.exception.GenerationRequestNotFoundException;
import today_store.content.request.exception.InvalidRequestBodyFormatException;

@DisplayName("전역 예외 처리기 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(new ValidationErrorResolver());

    @Test
    @DisplayName("커스텀 예외를 공통 에러 응답으로 변환")
    void shouldHandleCustomException() {
        // CustomException이 발생하면 정의된 상태 코드와 에러 코드로 응답을 만들어야 한다.

        // given
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException();

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("A004");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid or expired refresh token.");
    }

    @Test
    @DisplayName("일반 예외를 시스템 에러 응답으로 변환")
    void shouldHandleUnexpectedException() {
        // 처리되지 않은 일반 예외가 발생하면 500 시스템 에러 응답을 만들어야 한다.

        // given
        Exception exception = new IllegalStateException("unexpected");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("SYS001");
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    }

    @Test
    @DisplayName("검증 실패 예외를 필드 에러 응답으로 변환")
    void shouldHandleMethodArgumentNotValidException() throws Exception {
        // 요청 검증에 실패하면 필드별 오류 정보를 포함한 400 응답을 만들어야 한다.

        // given
        Method method = ValidationFixture.class.getDeclaredMethod("handle", LoginRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new LoginRequest("", ""), "loginRequest");
        bindingResult.addError(new FieldError(
                "loginRequest",
                "provider",
                "",
                false,
                null,
                null,
                "소셜 로그인 제공자(provider)는 필수 항목입니다."
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("A001");
        assertThat(response.getBody().getMessage()).isEqualTo("Missing required fields.");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("provider");
        assertThat(response.getBody().getErrors().get(0).getReason()).isEqualTo("소셜 로그인 제공자(provider)는 필수 항목입니다.");
    }

    @Test
    @DisplayName("생성 요청 검증 실패를 C001 응답으로 변환")
    void shouldHandleCreateGenerationValidationException() throws Exception {
        // 생성 요청 검증 실패는 C001 에러 응답으로 변환해야 한다.

        // given
        Method method = ValidationFixture.class.getDeclaredMethod("handleCreateGeneration", CreateGenerationRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                CreateGenerationRequest.builder().build(),
                "createGenerationRequest"
        );
        bindingResult.addError(new FieldError(
                "createGenerationRequest",
                "concept",
                null,
                false,
                null,
                null,
                "Concept is required"
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("C001");
        assertThat(response.getBody().getMessage()).isEqualTo("Missing required fields");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("concept");
    }

    @Test
    @DisplayName("생성 요청 수정 검증 실패를 C001 응답으로 변환")
    void shouldHandleUpdateGenerationValidationException() throws Exception {
        // 생성 요청 수정 검증 실패는 C001 에러 응답으로 변환해야 한다.

        // given
        Method method = ValidationFixture.class.getDeclaredMethod("handleUpdateGeneration", UpdateGenerationRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                UpdateGenerationRequest.builder().build(),
                "updateGenerationRequest"
        );
        bindingResult.addError(new FieldError(
                "updateGenerationRequest",
                "imageConfigs",
                "",
                false,
                null,
                null,
                "imageConfigs are required"
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("C001");
        assertThat(response.getBody().getMessage()).isEqualTo("Missing required fields");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("imageConfigs");
    }

    @Test
    @DisplayName("페이지 검증 실패를 P001 응답으로 변환")
    void shouldHandlePageRequestValidationException() throws Exception {
        // 페이지 파라미터 검증 실패는 P001 에러 응답으로 변환해야 한다.

        // given
        Method method = ValidationFixture.class.getDeclaredMethod("handlePageRequest", PageRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new PageRequest(), "pageRequest");
        bindingResult.addError(new FieldError(
                "pageRequest",
                "page",
                "0",
                false,
                null,
                null,
                "Page number should be greater than 1"
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("P001");
        assertThat(response.getBody().getMessage()).isEqualTo("invalid (page, size) parameter");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("page");
    }

    @Test
    @DisplayName("파일 크기 초과를 C004 응답으로 변환")
    void shouldHandleMaxUploadSizeExceededException() {
        // 업로드 파일 크기 초과는 C004 에러 응답으로 변환해야 한다.

        // given
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(10L);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMaxUploadSizeExceededException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("C004");
        assertThat(response.getBody().getMessage()).isEqualTo("File size limit exceeded");
    }

    @Test
    @DisplayName("파일명 불일치를 C002 응답으로 변환")
    void shouldHandleFileNameMismatchException() {
        // 파일명 불일치 예외는 C002 에러 응답으로 변환해야 한다.

        // given
        FileNameMismatchException exception = new FileNameMismatchException();

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("C002");
        assertThat(response.getBody().getMessage()).isEqualTo("File name mismatch in imageConfigs");
    }

    @Test
    @DisplayName("잘못된 요청 본문 형식을 C003 응답으로 변환")
    void shouldHandleInvalidRequestBodyFormatException() {
        // 잘못된 요청 본문 형식 예외는 C003 에러 응답으로 변환해야 한다.

        // given
        InvalidRequestBodyFormatException exception = new InvalidRequestBodyFormatException();

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("C003");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request body format");
    }

    @Test
    @DisplayName("생성 요청 없음 예외를 C005 응답으로 변환")
    void shouldHandleGenerationRequestNotFoundException() {
        // 생성 요청이 없으면 C005 에러 응답으로 변환해야 한다.

        // given
        GenerationRequestNotFoundException exception = new GenerationRequestNotFoundException();

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("C005");
        assertThat(response.getBody().getMessage()).isEqualTo("Request not found or already deleted");
    }

    @Test
    @DisplayName("리소스 소유권 불일치를 A008 응답으로 변환")
    void shouldHandleAccessDeniedToResourceException() {
        // 리소스 소유권 불일치 예외는 A008 에러 응답으로 변환해야 한다.

        // given
        AccessDeniedToResourceException exception = new AccessDeniedToResourceException();

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("A008");
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied. Resource ownership mismatch.");
    }

    @Test
    @DisplayName("사용자 프로필 검증 에러 코드 매핑")
    void shouldResolveUserValidationErrorCode() throws Exception {
        // 사용자 프로필 수정 요청 검증 실패는 U001 에러 코드로 응답해야 한다.

        // given
        Method method = ValidationFixture.class.getDeclaredMethod("handleUserProfile", UpdateUserProfileRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                UpdateUserProfileRequest.builder().name("a").build(),
                "updateUserProfileRequest"
        );
        bindingResult.addError(new FieldError(
                "updateUserProfileRequest",
                "name",
                "a",
                false,
                null,
                null,
                "Name must be between 2 and 100 characters"
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("U001");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid name format");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("name");
    }

    @Test
    @DisplayName("가게 생성 검증 에러 코드 매핑")
    void shouldResolveStoreValidationErrorCode() throws Exception {
        // 가게 생성 요청 검증 실패는 S001 에러 코드로 응답해야 한다.

        // given
        Method method = ValidationFixture.class.getDeclaredMethod("handleCreateStore", CreateStoreRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                CreateStoreRequest.builder().build(),
                "createStoreRequest"
        );
        bindingResult.addError(new FieldError(
                "createStoreRequest",
                "storeName",
                null,
                false,
                null,
                null,
                "Store name is required"
        ));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("S001");
        assertThat(response.getBody().getMessage()).isEqualTo("Store name and business type are required");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("storeName");
    }

    @Test
    @DisplayName("가게 중복 예외를 공통 에러 응답으로 변환")
    void shouldHandleStoreAlreadyExistException() {
        // 가게가 이미 존재하면 S002 에러 응답을 반환해야 한다.

        // given
        StoreAlreadyExistException exception = new StoreAlreadyExistException();

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("S002");
        assertThat(response.getBody().getMessage()).isEqualTo("User already has a registered store");
    }

    @Test
    @DisplayName("가게 없음 예외를 공통 에러 응답으로 변환")
    void shouldHandleStoreNotFoundException() {
        // 가게를 찾지 못하면 S003 에러 응답을 반환해야 한다.

        // given
        StoreNotFoundException exception = new StoreNotFoundException();

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("S003");
        assertThat(response.getBody().getMessage()).isEqualTo("Store profile not found");
    }

    private static class ValidationFixture {

        public void handle(@Valid LoginRequest loginRequest) {
        }

        public void handleUserProfile(@Valid UpdateUserProfileRequest updateUserProfileRequest) {
        }

        public void handleCreateStore(@Valid CreateStoreRequest createStoreRequest) {
        }

        public void handleCreateGeneration(@Valid CreateGenerationRequest createGenerationRequest) {
        }

        public void handleUpdateGeneration(@Valid UpdateGenerationRequest updateGenerationRequest) {
        }

        public void handlePageRequest(@Valid PageRequest pageRequest) {
        }
    }
}
