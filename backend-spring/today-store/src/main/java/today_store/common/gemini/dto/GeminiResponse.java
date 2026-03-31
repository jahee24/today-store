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
public class GeminiResponse {
    private List<Candidate> candidates;
    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;

    private Long responseTimeMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private Content content;
        private String finishReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private Integer promptTokenCount;
        @JsonProperty("candidatesTokenCount")
        private Integer candidatesTokenCount;
        @JsonProperty("totalTokenCount")
        private Integer totalTokenCount;
    }

    public String getText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Candidate firstCandidate = candidates.get(0);
        if (firstCandidate.getContent() == null || firstCandidate.getContent().getParts() == null || firstCandidate.getContent().getParts().isEmpty()) {
            return null;
        }
        return firstCandidate.getContent().getParts().get(0).getText();
    }
}
