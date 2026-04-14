package today_store.common.instagram.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class InstagramPublishRequest {
    @NotNull
    private UUID contentId;
}
