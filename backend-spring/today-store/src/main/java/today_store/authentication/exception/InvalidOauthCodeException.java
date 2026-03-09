package today_store.authentication.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class InvalidOauthCodeException extends CustomException {
    public InvalidOauthCodeException() {
        super(ErrorCode.INVALID_OAUTH_CODE);
    }
}
