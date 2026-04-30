package today_store.common.runcomfy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunComfyResultResponse {
    @JsonProperty("request_id")
    private String requestId;

    private String status;

    private Map<String, NodeOutput> outputs;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("finished_at")
    private String finishedAt;

    private ErrorInfo error;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeOutput {
        private List<ImageInfo> images;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageInfo {
        private String url;
        private String filename;
        private String subfolder;
        private String type;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private String error;
        private String details;
        private String debugInfo;
        private Integer errorCode;
    }
}
