package today_store.content.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {
    @JsonProperty("task_id")
    private UUID taskId;
    
    private String status;
    
    private String result;
    
    @JsonProperty("error_message")
    private String errorMessage;
}
