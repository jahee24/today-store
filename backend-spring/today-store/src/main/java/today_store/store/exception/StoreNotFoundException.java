package today_store.store.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class StoreNotFoundException extends CustomException {
    public StoreNotFoundException(){super(ErrorCode.STORE_NOT_FOUND);}
}
