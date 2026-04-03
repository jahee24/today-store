package today_store.content.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateContentResponse {
    private UUID requestId;
    private UUID taskId;
    private LocalDateTime startedAt;
}
