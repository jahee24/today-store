package today_store.content.publish.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.content.entity.ContentPlatform;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StartManualPublishRequest {
    @NotNull(message = "contentId is required")
    private UUID contentId;

    @NotNull(message = "platform is required")
    private ContentPlatform platform;
}
