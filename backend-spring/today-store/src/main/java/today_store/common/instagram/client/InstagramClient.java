package today_store.common.instagram.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramClient {

    private final WebClient webClient;

    @Value("${instagram.app-id}")
    private String appId;

    @Value("${instagram.app-secret}")
    private String appSecret;

    private static final String API_BASE_URL = "https://graph.instagram.com";
    private static final String AUTH_BASE_URL = "https://api.instagram.com";

     // Short-lived code -> Short-lived token
    public String getShortLivedToken(String authCode, String redirectUri) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", appId);
        formData.add("client_secret", appSecret);
        formData.add("grant_type", "authorization_code");
        formData.add("redirect_uri", redirectUri);
        formData.add("code", authCode);

        return webClient.post()
                .uri(AUTH_BASE_URL + "/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Failed to get short-lived token: {}", response.statusCode());
                    return Mono.error(new CustomException(ErrorCode.INSTAGRAM_INVALID_AUTH_CODE));
                })
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("access_token"))
                .block();
    }

     // Short-lived token -> Long-lived token
    public Map<String, Object> getLongLivedToken(String shortLivedToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("graph.instagram.com")
                        .path("/access_token")
                        .queryParam("grant_type", "ig_exchange_token")
                        .queryParam("client_secret", appSecret)
                        .queryParam("access_token", shortLivedToken)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> (Map<String, Object>) res)
                .block();
    }

    // Get Instagram User Info
    public Map<String, String> getUserInfo(String accessToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("graph.instagram.com")
                        .path("/me")
                        .queryParam("fields", "id,username")
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> Map.of(
                        "id", (String) res.get("id"),
                        "username", (String) res.get("username")
                ))
                .block();
    }

     // Create Media Container
    public String createMediaContainer(String instagramUserId, String accessToken, String imageUrl, String caption, boolean isCarouselItem) {
        return webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.scheme("https")
                            .host("graph.instagram.com")
                            .path("/{userId}/media")
                            .queryParam("image_url", imageUrl);
                    if (caption != null) {
                        uriBuilder.queryParam("caption", caption);
                    }
                    if (isCarouselItem) {
                        uriBuilder.queryParam("is_carousel_item", true);
                    }
                    uriBuilder.queryParam("access_token", accessToken);
                    return uriBuilder.build(instagramUserId);
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Failed to create media container: {}", response.statusCode());
                    return Mono.error(new CustomException(ErrorCode.INSTAGRAM_TOKEN_EXPIRED)); // Simplify for now
                })
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("id"))
                .block();
    }

     // Create Carousel Container
    public String createCarouselContainer(String instagramUserId, String accessToken, List<String> children, String caption) {
        String childrenStr = String.join(",", children);
        return webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.scheme("https")
                            .host("graph.instagram.com")
                            .path("/{userId}/media")
                            .queryParam("media_type", "CAROUSEL")
                            .queryParam("children", childrenStr);
                    if (caption != null) {
                        uriBuilder.queryParam("caption", caption);
                    }
                    uriBuilder.queryParam("access_token", accessToken);
                    return uriBuilder.build(instagramUserId);
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Failed to create carousel container: {}", response.statusCode());
                    return Mono.error(new CustomException(ErrorCode.INSTAGRAM_TOKEN_EXPIRED)); // Simplify for now
                })
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("id"))
                .block();
    }

     // Publish Media
    public String publishMedia(String instagramUserId, String accessToken, String creationId) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("graph.instagram.com")
                        .path("/{userId}/media_publish")
                        .queryParam("creation_id", creationId)
                        .queryParam("access_token", accessToken)
                        .build(instagramUserId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Failed to publish media: {}", response.statusCode());
                    return Mono.error(new CustomException(ErrorCode.INSTAGRAM_TOKEN_EXPIRED)); // Simplify for now
                })
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("id"))
                .block();
    }

     // Get Media Info (permalink, timestamp)
    public Map<String, Object> getMediaInfo(String mediaId, String accessToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("graph.instagram.com")
                        .path("/{mediaId}")
                        .queryParam("fields", "permalink,timestamp")
                        .queryParam("access_token", accessToken)
                        .build(mediaId))
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> (Map<String, Object>) res)
                .block();
    }
}
