package today_store.content.content.exception;


import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class InvalidRegenerationRequestException extends CustomException {
    public InvalidRegenerationRequestException() {
        super(ErrorCode.INVALID_REGENERATION_REQUEST);
    }
}

