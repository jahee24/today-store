package today_store.content.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentImage;
import today_store.content.content.entity.GenerationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {
    private UUID id;
    private UUID requestId;
    private GenerationType generationType;
    private Boolean isPosted;
    private LocalDateTime createdAt;
    private ContentData contentData;
    private List<ContentImageResponse> images;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentData {
        private PlatformData instagram;
        private PlatformData karrot;
        private PlatformData naver;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformData {
        private String text;
        private List<String> hashtags;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentImageResponse {
        private UUID id;
        private UUID inputImageId;
        private String url;
        private LocalDateTime createdAt;
    }

    public static ContentResponse from(Content content, List<ContentImage> images, boolean isPosted, Function<String, String> urlSigner) {
        return ContentResponse.builder()
                .id(content.getId())
                .requestId(content.getGenerationRequest().getId())
                .generationType(content.getGenerationType())
                .isPosted(isPosted)
                .createdAt(content.getCreatedAt())
                .contentData(ContentData.builder()
                        .instagram(PlatformData.builder()
                                .text(content.getInstagramText())
                                .hashtags(content.getInstagramHashtags())
                                .build())
                        .karrot(PlatformData.builder()
                                .text(content.getKarrotText())
                                .hashtags(content.getKarrotTags())
                                .build())
                        .naver(PlatformData.builder()
                                .text(content.getNaverText())
                                .hashtags(content.getNaverKeywords())
                                .build())
                        .build())
                .images(images.stream()
                        .map(img -> ContentImageResponse.builder()
                                .id(img.getId())
                                .inputImageId(img.getInputImageId())
                                .url(urlSigner != null ? urlSigner.apply(img.getUrl()) : img.getUrl())
                                .createdAt(img.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
