package today_store.common.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum RateLimitTier {
    HIGH(5, Duration.ofMinutes(1)),
    MIDDLE(30, Duration.ofMinutes(1)),
    LOW(100, Duration.ofMinutes(1));

    private final int capacity;
    private final Duration duration;
}
