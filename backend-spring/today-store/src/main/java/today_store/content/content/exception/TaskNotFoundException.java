package today_store.content.content.exception;


import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

public class TaskNotFoundException extends CustomException {
    public TaskNotFoundException(){super(ErrorCode.TASK_NOT_FOUND);}
}
