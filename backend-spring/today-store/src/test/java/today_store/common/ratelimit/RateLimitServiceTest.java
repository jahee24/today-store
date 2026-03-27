package today_store.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimit 서비스 테스트")
class RateLimitServiceTest {

    private final RateLimitService rateLimitService = new RateLimitService();

    @Test
    @DisplayName("같은 키와 티어는 용량만큼만 허용")
    void shouldAllowRequestsUpToCapacityWhenKeyAndTierAreSame() {
        // 같은 key와 tier 조합은 정의된 capacity까지만 요청을 허용해야 한다.

        // given
        boolean first = true;
        for (int i = 0; i < RateLimitTier.HIGH.getCapacity(); i++) {
            first = rateLimitService.tryConsume("same-key", RateLimitTier.HIGH);
        }

        // when
        boolean exceeded = rateLimitService.tryConsume("same-key", RateLimitTier.HIGH);

        // then
        assertThat(first).isTrue();
        assertThat(exceeded).isFalse();
    }

    @Test
    @DisplayName("키가 다르면 버킷 분리")
    void shouldSeparateBucketsWhenKeyIsDifferent() {
        // key가 다르면 같은 tier라도 서로 다른 버킷으로 동작해야 한다.

        // given
        for (int i = 0; i < RateLimitTier.HIGH.getCapacity(); i++) {
            rateLimitService.tryConsume("exhausted-key", RateLimitTier.HIGH);
        }

        // when
        boolean otherKeyAllowed = rateLimitService.tryConsume("other-key", RateLimitTier.HIGH);

        // then
        assertThat(otherKeyAllowed).isTrue();
    }

    @Test
    @DisplayName("티어가 다르면 버킷 분리")
    void shouldSeparateBucketsWhenTierIsDifferent() {
        // tier가 다르면 같은 key라도 서로 다른 버킷으로 동작해야 한다.

        // given
        for (int i = 0; i < RateLimitTier.HIGH.getCapacity(); i++) {
            rateLimitService.tryConsume("same-key", RateLimitTier.HIGH);
        }

        // when
        boolean lowerTierAllowed = rateLimitService.tryConsume("same-key", RateLimitTier.LOW);

        // then
        assertThat(lowerTierAllowed).isTrue();
    }
}
