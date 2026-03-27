package today_store.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimit 인터셉터 테스트")
class RateLimitInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        rateLimitInterceptor = new RateLimitInterceptor(rateLimitService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("어노테이션이 없으면 그대로 통과")
    void shouldPassThroughWhenHandlerHasNoRateLimitAnnotation() throws Exception {
        // RateLimit 어노테이션이 없는 핸들러는 레이트리밋 검사 없이 통과시켜야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = createHandlerMethod("open");

        // when
        boolean allowed = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(allowed).isTrue();
        then(rateLimitService).should(never()).tryConsume(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("레이트리밋이 허용되면 요청 통과")
    void shouldAllowRequestWhenRateLimitServiceAllowsIt() throws Exception {
        // RateLimit 어노테이션이 있는 요청이 허용 범위 안이면 그대로 통과시켜야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("127.0.0.1");
        HandlerMethod handlerMethod = createHandlerMethod("limited");
        given(rateLimitService.tryConsume("127.0.0.1", RateLimitTier.HIGH)).willReturn(true);

        // when
        boolean allowed = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(allowed).isTrue();
        then(rateLimitService).should().tryConsume("127.0.0.1", RateLimitTier.HIGH);
    }

    @Test
    @DisplayName("레이트리밋 초과 시 예외")
    void shouldThrowRateLimitExceededExceptionWhenRateLimitIsExceeded() throws Exception {
        // RateLimit 허용량을 초과한 요청은 레이트리밋 초과 예외를 던져야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("127.0.0.1");
        HandlerMethod handlerMethod = createHandlerMethod("limited");
        given(rateLimitService.tryConsume("127.0.0.1", RateLimitTier.HIGH)).willReturn(false);

        // when
        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitInterceptor.preHandle(request, response, handlerMethod)
        );

        // then
        assertThat(exception.getErrorCode().getCode()).isEqualTo("R001");
        assertThat(exception.getErrorCode().getMessage()).isEqualTo("Rate limit exceeded. Please try again later.");
    }

    @Test
    @DisplayName("인증된 사용자는 이메일을 식별자로 사용")
    void shouldUseAuthenticatedEmailWhenSecurityContextHasAuthentication() throws Exception {
        // 인증된 사용자가 있으면 IP보다 사용자 이메일을 우선 식별자로 사용해야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        HandlerMethod handlerMethod = createHandlerMethod("limited");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        given(rateLimitService.tryConsume("user@example.com", RateLimitTier.HIGH)).willReturn(true);

        // when
        boolean allowed = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(allowed).isTrue();
        then(rateLimitService).should().tryConsume("user@example.com", RateLimitTier.HIGH);
    }

    @Test
    @DisplayName("비인증 요청은 X-Forwarded-For 우선 사용")
    void shouldUseForwardedIpWhenAuthenticationIsMissing() throws Exception {
        // 인증 정보가 없으면 X-Forwarded-For 헤더 값을 식별자로 우선 사용해야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        request.setRemoteAddr("127.0.0.1");
        HandlerMethod handlerMethod = createHandlerMethod("limited");
        given(rateLimitService.tryConsume("203.0.113.10", RateLimitTier.HIGH)).willReturn(true);

        // when
        boolean allowed = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(allowed).isTrue();
        then(rateLimitService).should().tryConsume("203.0.113.10", RateLimitTier.HIGH);
    }

    private HandlerMethod createHandlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    private static class TestController {

        @RateLimit(tier = RateLimitTier.HIGH)
        public void limited() {
        }

        public void open() {
        }
    }
}
