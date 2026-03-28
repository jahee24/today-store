package today_store.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ProxyManager<String> proxyManager;

    public boolean tryConsume(String key, RateLimitTier tier) {
        String bucketKey = "ratelimit:" + key + ":" + tier.name();

        Supplier<BucketConfiguration> configurationSupplier = () -> createBucketConfiguration(tier);
        Bucket bucket = proxyManager.builder().build(bucketKey, configurationSupplier);

        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {} (Tier: {})", key, tier.name());
        }
        return allowed;
    }

    private BucketConfiguration createBucketConfiguration(RateLimitTier tier) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(tier.getCapacity())
                .refillIntervally(tier.getCapacity(), tier.getDuration())
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}
