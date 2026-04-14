package today_store.content.publish.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import today_store.authentication.entity.User;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentPost;
import today_store.content.content.entity.PublishStatus;
import today_store.content.content.exception.ContentNotFoundException;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.repository.ContentRepository;
import today_store.content.publish.dto.DeeplinkPublishRequest;
import today_store.content.publish.dto.DeeplinkPublishResponse;

@Service
@RequiredArgsConstructor
public class DeeplinkPublishService {

    private final ContentRepository contentRepository;
    private final ContentPostRepository contentPostRepository;
    private final DeepLinkGenerator deepLinkGenerator;

    @Transactional
    public DeeplinkPublishResponse generateDeeplink(User user, DeeplinkPublishRequest request) {
        Content content = contentRepository.findByIdAndIsDeletedFalse(request.getContentId())
                .orElseThrow(ContentNotFoundException::new);

        if (!content.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        // Reuse IN_PROGRESS post or create new one
        ContentPost contentPost = contentPostRepository
                .findFirstByContentAndPlatformAndStatusOrderByPublishedAtDesc(
                        content, request.getPlatform(), PublishStatus.IN_PROGRESS)
                .orElseGet(() -> contentPostRepository.save(
                        ContentPost.builder()
                                .content(content)
                                .platform(request.getPlatform())
                                .status(PublishStatus.IN_PROGRESS)
                                .build()
                ));

        String deeplinkUrl = deepLinkGenerator.generateDeeplink(content, request.getPlatform());
        String fallbackUrl = deepLinkGenerator.generateFallbackUrl(request.getPlatform());

        return DeeplinkPublishResponse.builder()
                .postId(contentPost.getId())
                .deeplinkUrl(deeplinkUrl)
                .fallbackUrl(fallbackUrl)
                .status(contentPost.getStatus())
                .build();
    }
}
