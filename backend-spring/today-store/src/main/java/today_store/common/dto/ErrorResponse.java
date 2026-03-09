package today_store.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FieldError> errors;
    
    private LocalDateTime timestamp;

    @Getter
    @Builder
    public static class FieldError {
        private String field;
        private String value;
        private String reason;
    }
}
