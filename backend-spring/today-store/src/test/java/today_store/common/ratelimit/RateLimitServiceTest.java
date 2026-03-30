package today_store.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimit 서비스 테스트")
class RateLimitServiceTest {

    private final Map<String, BucketProxy> buckets = new ConcurrentHashMap<>();
    private final ProxyManager<String> proxyManager = mock(ProxyManager.class);
    private final RateLimitService rateLimitService = new RateLimitService(proxyManager);

    @BeforeEach
    void setUp() {
        RemoteBucketBuilder<String> builder = mock(RemoteBucketBuilder.class);
        when(proxyManager.builder()).thenReturn(builder);

        when(builder.build(anyString(), any(Supplier.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Supplier<BucketConfiguration> configSupplier = invocation.getArgument(1);

            return buckets.computeIfAbsent(key, ignored -> {
                BucketConfiguration config = configSupplier.get();
                Bucket localBucket = Bucket.builder()
                        .addLimit(config.getBandwidths()[0])
                        .build();

                BucketProxy proxy = mock(BucketProxy.class);
                when(proxy.tryConsume(anyLong())).thenAnswer(inv -> localBucket.tryConsume(inv.getArgument(0, Long.class)));
                return proxy;
            });
        });
    }

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
