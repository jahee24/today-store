package today_store.authentication.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import jakarta.servlet.FilterChain;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import today_store.authentication.repository.BlacklistedTokenRepository;

import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 인증 필터 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, blacklistedTokenRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 미설정")
    void shouldNotSetAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
        // Authorization 헤더가 없는 요청은 인증 없이 다음 필터로 넘겨야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
        then(jwtTokenProvider).should(never()).validateToken(any());
    }

    @Test
    @DisplayName("블랙리스트 토큰이면 인증 미설정")
    void shouldNotSetAuthenticationWhenTokenIsBlacklisted() throws Exception {
        // 블랙리스트에 등록된 토큰은 인증을 설정하지 않고 그대로 통과시켜야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer blacklisted-token");
        given(blacklistedTokenRepository.existsById("blacklisted-token")).willReturn(true);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
        then(jwtTokenProvider).should(never()).validateToken(any());
    }

    @Test
    @DisplayName("유효한 토큰이면 인증 설정")
    void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
        // 유효한 JWT가 전달되면 인증 객체를 SecurityContext에 저장해야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        given(blacklistedTokenRepository.existsById("valid-token")).willReturn(false);
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getAuthentication("valid-token")).willReturn(authentication);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증 미설정")
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
        // 유효하지 않은 JWT가 전달되면 인증 객체를 저장하지 않아야 한다.

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer invalid-token");

        given(blacklistedTokenRepository.existsById("invalid-token")).willReturn(false);
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
        then(jwtTokenProvider).should(never()).getAuthentication(any());
    }
}
