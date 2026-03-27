package today_store.content.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.request.entity.GenerationRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGenerationResponse {
    private UUID requestId;
    private LocalDateTime createdAt;

    public static CreateGenerationResponse from(GenerationRequest request) {
        return CreateGenerationResponse.builder()
                .requestId(request.getId())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
