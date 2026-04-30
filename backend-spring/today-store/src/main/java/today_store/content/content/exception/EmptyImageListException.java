package today_store.content.content.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class EmptyImageListException extends CustomException {
    public EmptyImageListException() {
        super(ErrorCode.EMPTY_CONTENT_IMAGES);
    }
}
