package today_store.authentication.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class UserDisabledException extends CustomException {
    public UserDisabledException() {
        super(ErrorCode.USER_DISABLED);
    }
}
