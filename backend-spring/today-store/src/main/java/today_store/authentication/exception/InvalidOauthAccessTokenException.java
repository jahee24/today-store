package today_store.authentication.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class InvalidOauthAccessTokenException extends CustomException {
    public InvalidOauthAccessTokenException() {
        super(ErrorCode.INVALID_OAUTH_TOKEN);
    }
}
