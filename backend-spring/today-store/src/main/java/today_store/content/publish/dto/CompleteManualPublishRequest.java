package today_store.content.publish.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.content.entity.PublishStatus;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompleteManualPublishRequest {
    @NotNull(message = "postId is required")
    private UUID postId;

    @NotNull(message = "status is required")
    private PublishStatus status;

    private String postUrl;
}
