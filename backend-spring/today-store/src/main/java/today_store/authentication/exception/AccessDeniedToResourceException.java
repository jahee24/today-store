package today_store.authentication.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class AccessDeniedToResourceException extends CustomException {
    public AccessDeniedToResourceException() {
        super(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
    }
}
