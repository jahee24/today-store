package today_store.common.gemini.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiPromptRequest {
    private String storeName;
    private String businessType;
    private String address;
    private Double latitude;
    private Double longitude;
    private String targetAge;
    private String targetGender;
    private String concept;
    private String additionalNote;
    private List<String> imageDescriptions;
    private List<String> signedUrls;
}
