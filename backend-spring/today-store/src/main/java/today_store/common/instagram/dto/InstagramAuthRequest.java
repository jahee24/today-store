package today_store.common.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InstagramAuthRequest {
    @NotBlank
    private String authCode;

    @NotBlank
    private String redirectUri;
}
