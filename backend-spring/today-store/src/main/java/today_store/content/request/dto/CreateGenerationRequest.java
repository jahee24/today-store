package today_store.content.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGenerationRequest {
    @NotBlank(message = "Concept is required")
    private String concept;

    private String additionalNote;

    @NotEmpty(message = "Image descriptions are required")
    private List<String> imageDescriptions;

    @NotEmpty(message = "At least one image file is required")
    private List<MultipartFile> images;
}
