package today_store.authentication.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("JWT 토큰 제공자 테스트")
class JwtTokenProviderTest {

    @Test
    @DisplayName("액세스 토큰 생성 및 검증")
    void shouldCreateAndValidateAccessTokenWhenAuthenticationIsProvided() {
        // 인증 정보가 주어지면 유효한 액세스 토큰을 생성하고 검증할 수 있어야 한다.

        // given
        JwtTokenProvider tokenProvider = createTokenProvider(60L, 120L);
        Authentication authentication = createAuthentication();

        // when
        String accessToken = tokenProvider.createAccessToken(authentication);
        boolean valid = tokenProvider.validateToken(accessToken);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("같은 인증 정보로 연속 생성한 리프레시 토큰 고유성 보장")
    void shouldCreateUniqueRefreshTokensForSameAuthentication() {
        // 같은 인증 정보로 즉시 연속 발급해도 리프레시 토큰은 서로 달라야 한다.

        // given
        JwtTokenProvider tokenProvider = createTokenProvider(60L, 120L);
        Authentication authentication = createAuthentication();

        // when
        String first = tokenProvider.createRefreshToken(authentication);
        String second = tokenProvider.createRefreshToken(authentication);

        // then
        assertThat(first).isNotEqualTo(second);
        assertThat(tokenProvider.validateToken(first)).isTrue();
        assertThat(tokenProvider.validateToken(second)).isTrue();
    }

    @Test
    @DisplayName("토큰에서 인증 정보 복원")
    void shouldExtractAuthenticationWhenTokenIsValid() {
        // 유효한 토큰이 있으면 subject와 권한 정보를 다시 인증 객체로 복원해야 한다.

        // given
        JwtTokenProvider tokenProvider = createTokenProvider(60L, 120L);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );
        String accessToken = tokenProvider.createAccessToken(authentication);

        // when
        Authentication extractedAuthentication = tokenProvider.getAuthentication(accessToken);

        // then
        assertThat(extractedAuthentication.getName()).isEqualTo("user@example.com");
        assertThat(extractedAuthentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("유효하지 않은 토큰 거부")
    void shouldReturnFalseWhenTokenIsInvalid() {
        // 서명이 맞지 않거나 형식이 잘못된 토큰은 유효하지 않다고 판단해야 한다.

        // given
        JwtTokenProvider tokenProvider = createTokenProvider(60L, 120L);

        // when
        boolean valid = tokenProvider.validateToken("invalid-token");

        // then
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("유효한 토큰의 남은 만료 시간 반환")
    void shouldReturnPositiveRemainingExpirationWhenTokenIsValid() {
        // 유효한 토큰에 대해서는 남은 만료 시간이 0보다 큰 값으로 계산되어야 한다.

        // given
        JwtTokenProvider tokenProvider = createTokenProvider(60L, 120L);
        String accessToken = tokenProvider.createAccessToken(createAuthentication());

        // when
        Long remainingExpiration = tokenProvider.getRemainingExpirationMillis(accessToken);

        // then
        assertThat(remainingExpiration).isGreaterThan(0L);
    }

    @Test
    @DisplayName("만료된 토큰의 남은 만료 시간을 0으로 반환")
    void shouldReturnZeroRemainingExpirationWhenTokenIsExpired() {
        // 이미 만료된 토큰에 대해서는 남은 만료 시간을 0으로 반환해야 한다.

        // given
        JwtTokenProvider expiredTokenProvider = createTokenProvider(-1L, 120L);
        String expiredToken = expiredTokenProvider.createAccessToken(createAuthentication());

        // when
        Long remainingExpiration = expiredTokenProvider.getRemainingExpirationMillis(expiredToken);

        // then
        assertThat(remainingExpiration).isZero();
    }

    private JwtTokenProvider createTokenProvider(long accessTokenExpirationSeconds, long refreshTokenExpirationSeconds) {
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secret", "dG9kYXktc3RvcmUtand0LXNlY3JldC1kdW1teS1rZXktZm9yLXRlc3Q=");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpirationSeconds", accessTokenExpirationSeconds);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationSeconds", refreshTokenExpirationSeconds);
        tokenProvider.init();
        return tokenProvider;
    }

    private Authentication createAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
