package today_store.content.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequestListResponse {
    private List<GenerationRequestSummary> data;
    private PaginationInfo pagination;

    public static GenerationRequestListResponse from(List<GenerationRequestSummary> data, PaginationInfo pagination) {
        return GenerationRequestListResponse.builder()
                .data(data)
                .pagination(pagination)
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationRequestSummary {
        private UUID requestId;
        private String concept;
        private String thumbnailUrl;
        private Integer imageCount;
        private LocalDateTime createdAt;

        public static GenerationRequestSummary from(GenerationRequest request, String thumbnailUrl, int imageCount) {
            return GenerationRequestSummary.builder()
                    .requestId(request.getId())
                    .concept(request.getConcept())
                    .thumbnailUrl(thumbnailUrl)
                    .imageCount(imageCount)
                    .createdAt(request.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private Integer currentPage;
        private Integer pageSize;
        private Long totalCount;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;

        public static PaginationInfo from(Page<?> page) {
            return PaginationInfo.builder()
                    .currentPage(page.getNumber() + 1)
                    .pageSize(page.getSize())
                    .totalCount(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }
    }
}