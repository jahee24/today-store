package today_store.common.gemini.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRegenerationRequest {
    private String originalInstagramText;
    private String originalKarrotText;
    private String originalNaverText;
    private String feedback;
    private List<String> targets;
}
