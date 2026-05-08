package today_store.common.runcomfy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunComfyInferenceResponse {
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("status_url")
    private String statusUrl;

    @JsonProperty("result_url")
    private String resultUrl;

    @JsonProperty("cancel_url")
    private String cancelUrl;
}
