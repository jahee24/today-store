package today_store.content.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageConfig {
    private UUID id;
    private String fileName;
    private String description;
    private Integer displayOrder;
}