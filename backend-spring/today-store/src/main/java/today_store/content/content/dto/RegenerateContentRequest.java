package today_store.content.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class
RegenerateContentRequest {
    @NotBlank(message = "Feedback is required")
    private String feedback;
    
    private boolean regenerateImage;
    
    @NotEmpty(message = "Target platforms are required")
    private List<String> targets;
}
