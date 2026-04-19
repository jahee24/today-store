package today_store.content.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class
RegenerateContentRequest {
    @NotBlank(message = "Feedback is required")
    private String feedback;

    private boolean regenerateImage;

    @NotBlank(message = "Target platform is required")
    @Pattern(
            regexp = "INSTAGRAM|KARROT|NAVER",
            message = "Target platform must be one of INSTAGRAM, KARROT, NAVER"
    )
    private String target;
}
