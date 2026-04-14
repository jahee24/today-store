package today_store.common.instagram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramAuthResponse {
    private String instagramUserId;
    private String username;
}
