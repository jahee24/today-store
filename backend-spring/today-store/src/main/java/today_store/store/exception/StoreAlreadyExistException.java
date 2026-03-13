package today_store.store.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class StoreAlreadyExistException extends CustomException {
    public StoreAlreadyExistException(){super(ErrorCode.STORE_ALREADY_EXISTS);}
}
