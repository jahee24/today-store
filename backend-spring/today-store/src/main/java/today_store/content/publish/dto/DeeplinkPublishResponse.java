package today_store.content.publish.dto;

import lombok.*;
import today_store.content.content.entity.PublishStatus;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeeplinkPublishResponse {
    private UUID postId;
    private String deeplinkUrl;
    private String fallbackUrl;
    private PublishStatus status;
}
