package today_store.common.ratelimit;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class RateLimitExceededException extends CustomException {
    public RateLimitExceededException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
