package today_store.common.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerationRequest {
    private List<Content> contents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
        @JsonProperty("file_data")
        private FileData fileData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileData {
        @JsonProperty("mime_type")
        private String mimeType;
        @JsonProperty("file_uri")
        private String fileUri;
    }
}
