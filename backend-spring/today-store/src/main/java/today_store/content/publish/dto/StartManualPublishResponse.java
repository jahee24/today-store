package today_store.content.publish.dto;

import lombok.*;
import today_store.content.content.entity.PublishStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StartManualPublishResponse {
    private UUID postId;
    private String text;
    private List<String> tags;
    private List<String> imageUrls;
    private PublishStatus status;
    private boolean alreadyPublished;
    private LocalDateTime lastPublishedAt;
}
