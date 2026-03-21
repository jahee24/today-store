package today_store.content.request.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGenerationRequest {
    private String concept;
    private String additionalNote;
    private String targetAge;
    private String targetGender;
    private String imageConfigs;   // JSON array of ImageConfig
    private List<MultipartFile> newImages;
}