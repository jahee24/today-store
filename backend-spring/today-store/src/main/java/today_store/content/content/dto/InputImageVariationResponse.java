package today_store.content.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.content.content.entity.InputImageVariation;

import java.util.UUID;
import java.util.function.Function;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputImageVariationResponse {
    private UUID id;
    private String url;
    private String angleType;

    public static InputImageVariationResponse from(InputImageVariation variation, Function<String, String> signedUrlGenerator) {
        return InputImageVariationResponse.builder()
                .id(variation.getId())
                .url(signedUrlGenerator.apply(variation.getUrl()))
                .angleType(variation.getAngleType())
                .build();
    }
}
