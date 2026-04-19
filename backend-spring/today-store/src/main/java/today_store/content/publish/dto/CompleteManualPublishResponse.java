package today_store.content.publish.dto;

import lombok.*;
import today_store.content.content.entity.PublishStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompleteManualPublishResponse {
    private UUID postId;
    private PublishStatus status;
    private LocalDateTime publishedAt;
}
