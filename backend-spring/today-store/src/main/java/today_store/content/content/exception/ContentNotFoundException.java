package today_store.content.content.exception;


import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class ContentNotFoundException extends CustomException {
    public ContentNotFoundException(){super(ErrorCode.CONTENT_NOT_FOUND);}
}
