package today_store.content.publish.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import today_store.authentication.entity.User;
import today_store.authentication.exception.AccessDeniedToResourceException;
import today_store.common.gcs.GcsService;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentImage;
import today_store.content.content.entity.ContentPost;
import today_store.content.content.entity.PublishStatus;
import today_store.content.content.repository.ContentImageRepository;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.repository.ContentRepository;
import today_store.content.content.exception.ContentNotFoundException;
import today_store.content.content.exception.PostNotFoundException;
import today_store.content.publish.dto.CompleteManualPublishRequest;
import today_store.content.publish.dto.CompleteManualPublishResponse;
import today_store.content.publish.dto.StartManualPublishRequest;
import today_store.content.publish.dto.StartManualPublishResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManualPublishService {

    private final ContentRepository contentRepository;
    private final ContentPostRepository contentPostRepository;
    private final ContentImageRepository contentImageRepository;
    private final GcsService gcsService;

    @Transactional
    public StartManualPublishResponse startManualPublish(User user, StartManualPublishRequest request) {
        Content content = contentRepository.findByIdAndIsDeletedFalse(request.getContentId())
                .orElseThrow(ContentNotFoundException::new);

        if (!content.getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        // Check if there is already an IN_PROGRESS post for this content and platform
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

        // Check if there is already a COMPLETED post for this content and platform
        var lastCompletedPost = contentPostRepository
                .findFirstByContentAndPlatformAndStatusOrderByPublishedAtDesc(
                        content, request.getPlatform(), PublishStatus.COMPLETED);

        String text = "";
        List<String> tags = List.of();

        switch (request.getPlatform()) {
            case INSTAGRAM -> {
                text = content.getInstagramText();
                tags = content.getInstagramHashtags();
            }
            case NAVER -> {
                text = content.getNaverText();
                tags = content.getNaverKeywords();
            }
            case KARROT -> {
                text = content.getKarrotText();
                tags = content.getKarrotTags();
            }
        }

        // Fetch image URLs
        List<ContentImage> images = contentImageRepository.findByContentOrderByCreatedAtAsc(content);
        List<String> imageUrls = images.stream()
                .map(img -> gcsService.generateSignedUrl(img.getUrl()))
                .collect(Collectors.toList());

        return StartManualPublishResponse.builder()
                .postId(contentPost.getId())
                .text(text)
                .tags(tags)
                .imageUrls(imageUrls)
                .status(contentPost.getStatus())
                .alreadyPublished(lastCompletedPost.isPresent())
                .lastPublishedAt(lastCompletedPost.map(ContentPost::getPublishedAt).orElse(null))
                .build();
    }

    @Transactional
    public CompleteManualPublishResponse completeManualPublish(User user, CompleteManualPublishRequest request) {
        ContentPost contentPost = contentPostRepository.findById(request.getPostId())
                .orElseThrow(PostNotFoundException::new);

        if (!contentPost.getContent().getGenerationRequest().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedToResourceException();
        }

        if (request.getStatus() == PublishStatus.COMPLETED) {
            contentPost.complete(request.getPostUrl());
        } else {
            contentPost.fail();
        }
        
        ContentPost updatedPost = contentPostRepository.save(contentPost);

        return CompleteManualPublishResponse.builder()
                .postId(updatedPost.getId())
                .status(updatedPost.getStatus())
                .publishedAt(updatedPost.getPublishedAt())
                .build();
    }
}
