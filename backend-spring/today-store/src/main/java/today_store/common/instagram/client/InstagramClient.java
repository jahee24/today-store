package today_store.common.instagram.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.instagram.exception.InstagramProcessingException;

import java.time.Duration;
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
                .onStatus(HttpStatusCode::is4xxClientError, this::handleInstagramError)
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("access_token"))
                .block();
    }

    // Short-lived token -> Long-lived token
    public Map<String, Object> getLongLivedToken(String shortLivedToken) {
        String uri = String.format("https://graph.instagram.com/access_token" +
                        "?grant_type=ig_exchange_token" +
                        "&client_secret=%s" +
                        "&access_token=%s",
                appSecret, shortLivedToken);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> (Map<String, Object>) res)
                .block();
    }

    // Get Instagram User Info
    public Map<String, String> getUserInfo(String accessToken) {
        String uri = "https://graph.instagram.com/v25.0/me?fields=user_id,username";

        return webClient.get()
                .uri(uri)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleInstagramError)
                .bodyToMono(Map.class)
                .map(res -> {
                    Object idObj = res.get("user_id") != null ? res.get("user_id") : res.get("id");
                    String id = idObj != null ? idObj.toString() : null;
                    String username = (String) res.get("username");

                    if (id == null || username == null) {
                        log.error("Missing required fields in Instagram response: {}", res);
                        throw new CustomException(ErrorCode.INSTAGRAM_INVALID_AUTH_CODE);
                    }

                    return Map.of("id", id, "username", username);
                })
                .block();
    }

    // Create Media Container
    public String createMediaContainer(String instagramUserId, String accessToken, String imageUrl, String caption, boolean isCarouselItem) {
        String uri = String.format("https://graph.instagram.com/v25.0/%s/media", instagramUserId);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("image_url", imageUrl);
        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption);
        }
        if (isCarouselItem) {
            body.put("isCarouselItem", true);
        }

        return webClient.post()
                .uri(uri)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleInstagramError)
                .bodyToMono(Map.class)
                .map(res -> {
                    String id = (String) res.get("id");
                    log.info("Successfully created Instagram media container. ID: {}", id);
                    return id;
                })
                .block();
    }

    // Create Carousel Container
    public String createCarouselContainer(String instagramUserId, String accessToken, List<String> children, String caption) {
        String uri = String.format("https://graph.instagram.com/v25.0/%s/media", instagramUserId);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("media_type", "CAROUSEL");
        body.put("children", String.join(",", children));
        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption);
        }

        return webClient.post()
                .uri(uri)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleInstagramError)
                .bodyToMono(Map.class)
                .map(res -> {
                    String id = (String) res.get("id");
                    log.info("Successfully created Instagram carousel container. ID: {}", id);
                    return id;
                })
                .block();
    }

    // Publish Media
    public String publishMedia(String instagramUserId, String accessToken, String creationId) {
        String uri = String.format("https://graph.instagram.com/v25.0/%s/media_publish", instagramUserId);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("creation_id", creationId);

        return webClient.post()
                .uri(uri)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleInstagramError)
                .bodyToMono(Map.class)
                .map(res -> {
                    String id = (String) res.get("id");
                    log.info("Successfully published Instagram media. Media ID: {}", id);
                    return id;
                })
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5))
                        .filter(throwable -> throwable instanceof InstagramProcessingException)
                        .doBeforeRetry(retrySignal -> log.warn("Instagram media not ready yet. Retrying... ({}/3)", retrySignal.totalRetries() + 1)))
                .block();
    }

    // Get Media Info (permalink, timestamp)
    public Map<String, Object> getMediaInfo(String mediaId, String accessToken) {
        String uri = String.format("https://graph.instagram.com/v25.0/%s?fields=permalink,timestamp", mediaId);

        return webClient.get()
                .uri(uri)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleInstagramError)
                .bodyToMono(Map.class)
                .map(res -> {
                    log.info("Successfully retrieved Instagram media info: {}", res);
                    return (Map<String, Object>) res;
                })
                .block();
    }

    private Mono<? extends Throwable> handleInstagramError(ClientResponse response) {
        return response.bodyToMono(Map.class).flatMap(body -> {
            log.error("Instagram API Error. Status: {}, Body: {}", response.statusCode(), body);

            Map<String, Object> errorMap = (Map<String, Object>) body.get("error");
            if (errorMap == null) {
                return Mono.error(new CustomException(ErrorCode.INSTAGRAM_API_ERROR, "Unknown error"));
            }

            String message = (String) errorMap.get("message");
            Number code = (Number) errorMap.get("code");

            // Retryable error: Media ID is not available (processing)
            if (code != null && code.intValue() == 9007) {
                return Mono.error(new InstagramProcessingException(message));
            }

            // Token Expired / Invalid
            if (response.statusCode() == HttpStatus.UNAUTHORIZED || (code != null && (code.intValue() == 190 || code.intValue() == 102))) {
                return Mono.error(new CustomException(ErrorCode.INSTAGRAM_TOKEN_EXPIRED));
            }

            // Permission / Scope issues
            if (response.statusCode() == HttpStatus.FORBIDDEN || (code != null && (code.intValue() == 10 || code.intValue() == 200))) {
                return Mono.error(new CustomException(ErrorCode.INSTAGRAM_PERMISSION_DENIED));
            }

            // Rate Limit
            if (response.statusCode() == HttpStatus.TOO_MANY_REQUESTS || (code != null && (code.intValue() == 4 || code.intValue() == 17 || code.intValue() == 32))) {
                return Mono.error(new CustomException(ErrorCode.INSTAGRAM_RATE_LIMIT_EXCEEDED));
            }

            // Generic API Error with original message
            return Mono.error(new CustomException(ErrorCode.INSTAGRAM_API_ERROR, message));
        });
    }
}
