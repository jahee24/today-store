package today_store.content.content.exception;


import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class PostNotFoundException extends CustomException {
    public PostNotFoundException() {
        super(ErrorCode.POST_NOT_FOUND);
    }
}
