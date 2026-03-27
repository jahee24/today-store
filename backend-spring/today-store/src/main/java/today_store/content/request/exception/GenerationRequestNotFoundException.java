package today_store.content.request.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class GenerationRequestNotFoundException extends CustomException {
    public GenerationRequestNotFoundException() {
        super(ErrorCode.GENERATION_REQUEST_NOT_FOUND);
    }
}
