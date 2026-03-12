package today_store.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, RateLimitTier tier) {
        String bucketKey = key + ":" + tier.name();
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createNewBucket(tier));
        
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {} (Tier: {})", key, tier.name());
        }
        return allowed;
    }

    private Bucket createNewBucket(RateLimitTier tier) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(tier.getCapacity())
                .refillIntervally(tier.getCapacity(), tier.getDuration())
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
