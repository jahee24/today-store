package today_store.content.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequestDetailResponse {
    private UUID id;
    private UUID userId;
    private String concept;
    private String additionalNote;
    private String targetAge;
    private String targetGender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ImageResponse> images;

    public static GenerationRequestDetailResponse from(GenerationRequest request, List<ImageResponse> images) {
        return GenerationRequestDetailResponse.builder()
                .id(request.getId())
                .userId(request.getUser().getId())
                .concept(request.getConcept())
                .additionalNote(request.getAdditionalNote())
                .targetAge(request.getTargetAge())
                .targetGender(request.getTargetGender())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .images(images)
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResponse {
        private UUID id;
        private String url;
        private String description;
        private Integer displayOrder;
        private LocalDateTime createdAt;

        public static ImageResponse from(InputImage image, String signedUrl) {
            return ImageResponse.builder()
                    .id(image.getId())
                    .url(signedUrl)
                    .description(image.getDescription())
                    .displayOrder(image.getDisplayOrder())
                    .createdAt(image.getCreatedAt())
                    .build();
        }
    }
}