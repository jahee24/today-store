package today_store.common.instagram.dto;

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
public class InstagramPublishResponse {
    private String status;
    private UUID postId;
    private String mediaId;
    private String link;
    private LocalDateTime publishedAt;
}
