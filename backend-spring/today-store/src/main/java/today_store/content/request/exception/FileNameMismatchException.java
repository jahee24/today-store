package today_store.content.request.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class FileNameMismatchException extends CustomException {
    public FileNameMismatchException() {
        super(ErrorCode.FILE_NAME_MISMATCH);
    }
}
