package today_store.content.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.content.entity.Content;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentListResponse {
    private UUID requestId;
    private List<ContentSummary> contents;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentSummary {
        private UUID contentId;
        private Boolean isPosted;
        private String instagramPreview;
        private String karrotPreview;
        private String naverPreview;
        private LocalDateTime createdAt;
    }

    public static ContentListResponse from(UUID requestId, List<Content> contents, Set<UUID> postedContentIds) {
        return ContentListResponse.builder()
                .requestId(requestId)
                .contents(contents.stream()
                        .map(c -> ContentSummary.builder()
                                .contentId(c.getId())
                                .isPosted(postedContentIds.contains(c.getId()))
                                .instagramPreview(makePreview(c.getInstagramText()))
                                .karrotPreview(makePreview(c.getKarrotText()))
                                .naverPreview(makePreview(c.getNaverText()))
                                .createdAt(c.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private static String makePreview(String text) {
        if (text == null || text.isEmpty()) return "";
        String singleLine = text.replace("\n", " ").replace("\r", " ").trim();
        if (singleLine.length() <= 30) return singleLine;
        return singleLine.substring(0, 30) + "...";
    }
}
