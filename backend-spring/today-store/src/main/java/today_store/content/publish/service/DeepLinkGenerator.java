package today_store.content.publish.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentPlatform;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static today_store.content.content.entity.ContentPlatform.INSTAGRAM;

@Component
@Slf4j
public class DeepLinkGenerator {

    public String generateDeeplink(Content content, ContentPlatform platform) {
        String text = switch (platform) {
            case INSTAGRAM -> content.getInstagramText();
            case NAVER -> content.getNaverText();
            case NAVER_PLACE -> content.getNaverText();
            case KARROT -> content.getKarrotText();
        };

        String encodedText = URLEncoder.encode(text != null ? text : "", StandardCharsets.UTF_8);

        return switch (platform) {
            case KARROT -> "daangn://post?text=" + encodedText;
            case NAVER_PLACE -> "naverplace://write?content=" + encodedText;
            case NAVER -> "naverblog://write?text=" + encodedText;
            case INSTAGRAM -> "instagram://library?Caption=" + encodedText;
            default -> "";
        };
    }

    public String generateFallbackUrl(ContentPlatform platform) {
        return switch (platform) {
            case KARROT -> "https://www.daangn.com/";
            case NAVER_PLACE -> "https://m.place.naver.com/";
            case NAVER -> "https://section.blog.naver.com/";
            case INSTAGRAM -> "https://www.instagram.com/";
            default -> "";
        };
    }
}
