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
import today_store.authentication.dto.LoginRequest;
import today_store.authentication.exception.InvalidRefreshTokenException;
import today_store.common.dto.ErrorResponse;

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

    private static class ValidationFixture {

        public void handle(@Valid LoginRequest loginRequest) {
        }
    }
}
