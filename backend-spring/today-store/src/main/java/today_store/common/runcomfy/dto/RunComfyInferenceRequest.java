package today_store.common.runcomfy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunComfyInferenceRequest {
    private Map<String, Object> overrides;
    
    @JsonProperty("workflow_api_json")
    private Map<String, Object> workflowApiJson;
}
