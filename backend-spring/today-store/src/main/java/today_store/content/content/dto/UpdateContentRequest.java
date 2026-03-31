package today_store.content.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContentRequest {
    private PlatformUpdate instagram;
    private PlatformUpdate karrot;
    private PlatformUpdate naver;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformUpdate {
        private String text;
        private List<String> hashtags;
    }
}
