package today_store.authentication.exception;

import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class UnsupportedProviderException extends CustomException {
    public UnsupportedProviderException() {
        super(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }
}
