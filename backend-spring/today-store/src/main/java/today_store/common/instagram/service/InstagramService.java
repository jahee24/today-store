package today_store.common.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import today_store.authentication.entity.User;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.instagram.client.InstagramClient;
import today_store.common.instagram.dto.InstagramAuthRequest;
import today_store.common.instagram.dto.InstagramAuthResponse;
import today_store.common.instagram.dto.InstagramPublishRequest;
import today_store.common.instagram.dto.InstagramPublishResponse;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentImage;
import today_store.content.content.entity.ContentPlatform;
import today_store.content.content.entity.ContentPost;
import today_store.content.content.repository.ContentImageRepository;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.repository.ContentRepository;
import today_store.content.publish.service.PublishPostService;
import today_store.user.entity.UserSocialAccount;
import today_store.user.repository.UserSocialAccountRepository;
import today_store.user.service.UserSocialAccountService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramService {

    private final InstagramClient instagramClient;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final UserSocialAccountService userSocialAccountService;
    private final ContentRepository contentRepository;
    private final ContentImageRepository contentImageRepository;
    private final PublishPostService publishPostService;

    private static final String PLATFORM_NAME = "INSTAGRAM";

    public InstagramAuthResponse authenticate(User user, InstagramAuthRequest request) {
        // 1. Exchange short-lived code for short-lived token
        String shortLivedToken = instagramClient.getShortLivedToken(request.getAuthCode(), request.getRedirectUri());

        // 2. Exchange short-lived token for long-lived token
        Map<String, Object> tokenInfo = instagramClient.getLongLivedToken(shortLivedToken);
        String longLivedToken = (String) tokenInfo.get("access_token");
        Number expiresIn = (Number) tokenInfo.get("expires_in");
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn.longValue());

        // 3. Get Instagram User Info
        Map<String, String> userInfo = instagramClient.getUserInfo(longLivedToken);
        String instagramUserId = userInfo.get("id");
        String username = userInfo.get("username");

        // 4. Link account (This part is DB-heavy and is handled within a separate transaction)
        userSocialAccountService.linkAccount(user, PLATFORM_NAME, instagramUserId, username, longLivedToken, expiresAt);

        return InstagramAuthResponse.builder()
                .instagramUserId(instagramUserId)
                .username(username)
                .build();
    }

    public InstagramPublishResponse publish(User user, InstagramPublishRequest request) {
        // 1. Find Content
        Content content = contentRepository.findById(request.getContentId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTENT_NOT_FOUND));

        // Ownership check
        if (!content.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED_TO_RESOURCE);
        }

        // 2. Find Instagram Account
        UserSocialAccount socialAccount = userSocialAccountRepository.findByUserAndPlatform(user, PLATFORM_NAME)
                .orElseThrow(() -> new CustomException(ErrorCode.INSTAGRAM_NOT_LINKED));

        if (!socialAccount.getIsValid() || (socialAccount.getTokenExpiresAt() != null && socialAccount.getTokenExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new CustomException(ErrorCode.INSTAGRAM_TOKEN_EXPIRED);
        }

        // 3. Get Content Images
        List<ContentImage> images = contentImageRepository.findByContentId(content.getId());
        if (images.isEmpty()) {
            throw new CustomException(ErrorCode.CONTENT_NOT_FOUND);
        }
        if (images.size() > 10) {
            log.warn("Instagram supports up to 10 images. Current count: {}", images.size());
            images = images.subList(0, 10);
        }

        String caption = content.getInstagramText();
        if (content.getInstagramHashtags() != null && !content.getInstagramHashtags().isEmpty()) {
            caption += "\n\n" + String.join(" ", content.getInstagramHashtags());
        }

        // Create In-Progress post (Transaction propagation: REQUIRES_NEW)
        ContentPost post = publishPostService.createInProgressPost(content, ContentPlatform.INSTAGRAM);

        try {
            String creationId;
            if (images.size() == 1) {
                // Single Image Post
                creationId = instagramClient.createMediaContainer(
                        socialAccount.getSocialUserId(),
                        socialAccount.getAccessToken(),
                        images.get(0).getUrl(),
                        caption,
                        false
                );
            } else {
                // Carousel Post
                List<String> itemContainerIds = images.stream()
                        .map(image -> instagramClient.createMediaContainer(
                                socialAccount.getSocialUserId(),
                                socialAccount.getAccessToken(),
                                image.getUrl(),
                                null, 
                                true
                        ))
                        .toList();

                creationId = instagramClient.createCarouselContainer(
                        socialAccount.getSocialUserId(),
                        socialAccount.getAccessToken(),
                        itemContainerIds,
                        caption
                );
            }

            // 4. Final Publish
            String mediaId = instagramClient.publishMedia(socialAccount.getSocialUserId(), socialAccount.getAccessToken(), creationId);

            // 5. Get Media Info
            Map<String, Object> mediaInfo = instagramClient.getMediaInfo(mediaId, socialAccount.getAccessToken());
            String permalink = (String) mediaInfo.get("permalink");
            String timestampStr = (String) mediaInfo.get("timestamp"); 
            LocalDateTime publishedAt = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);

            // 6. Complete ContentPost (Transaction propagation: REQUIRES_NEW)
            publishPostService.completePost(post.getId(), mediaId, permalink, publishedAt);

            return InstagramPublishResponse.builder()
                    .status("success")
                    .postId(post.getId())
                    .mediaId(mediaId)
                    .link(permalink)
                    .publishedAt(publishedAt)
                    .build();

        } catch (Exception e) {
            log.error("Failed to publish to Instagram for content: {}", content.getId(), e);
            // Fail ContentPost
            publishPostService.failPost(post.getId());
            throw e;
        }
    }
}
