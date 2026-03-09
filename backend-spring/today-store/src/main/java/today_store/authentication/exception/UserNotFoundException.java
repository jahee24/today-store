package today_store.authentication.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class UserNotFoundException extends CustomException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
