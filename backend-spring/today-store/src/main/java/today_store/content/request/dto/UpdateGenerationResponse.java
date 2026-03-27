package today_store.content.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.request.entity.GenerationRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGenerationResponse {
    private UUID requestId;
    private String concept;
    private String additionalNote;
    private String targetAge;
    private String targetGender;
    private LocalDateTime updatedAt;
    private ImageSummary imageSummary;

    public static UpdateGenerationResponse from(GenerationRequest request, ImageSummary imageSummary) {
        return UpdateGenerationResponse.builder()
                .requestId(request.getId())
                .concept(request.getConcept())
                .additionalNote(request.getAdditionalNote())
                .targetAge(request.getTargetAge())
                .targetGender(request.getTargetGender())
                .updatedAt(request.getUpdatedAt())
                .imageSummary(imageSummary)
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageSummary {
        private Integer totalCount;
        private Integer addedCount;
        private Integer deletedCount;

        public static ImageSummary from(int totalCount, int addedCount, int deletedCount) {
            return ImageSummary.builder()
                    .totalCount(totalCount)
                    .addedCount(addedCount)
                    .deletedCount(deletedCount)
                    .build();
        }
    }
}