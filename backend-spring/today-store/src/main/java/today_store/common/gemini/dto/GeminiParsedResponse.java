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
public class GeminiParsedResponse {
    @JsonProperty("photo_info")
    private String photoInfo;
    private String text;
    private List<String> hashtags;

    // Metadata
    private Integer inputTokens;
    private Integer outputTokens;
    private Long responseTimeMs;
}
