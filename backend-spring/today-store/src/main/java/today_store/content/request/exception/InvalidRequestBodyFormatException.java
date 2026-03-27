package today_store.content.request.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class InvalidRequestBodyFormatException extends CustomException {
    public InvalidRequestBodyFormatException() {
        super(ErrorCode.INVALID_REQUEST_BODY_FORMAT);
    }
}
