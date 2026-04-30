package today_store.content.content.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContentImagesRequest {
    @NotEmpty
    private List<ImageSourceItem> images;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageSourceItem {
        private SourceType type;
        private UUID id; // EXISTING(ContentImage ID), ORIGINAL(InputImage ID), VARIATION(InputImageVariation ID)
    }

    public enum SourceType {
        EXISTING, ORIGINAL, VARIATION
    }
}
