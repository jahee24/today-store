package today_store.content.content.exception;


import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class GenerationAlreadyInProgressException extends CustomException {
    public GenerationAlreadyInProgressException(){super(ErrorCode.GENERATION_ALREADY_IN_PROGRESS);}
}
