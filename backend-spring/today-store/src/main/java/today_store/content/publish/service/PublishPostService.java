package today_store.content.publish.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentPlatform;
import today_store.content.content.entity.ContentPost;
import today_store.content.content.entity.PublishStatus;
import today_store.content.content.repository.ContentPostRepository;
import today_store.content.content.exception.PostNotFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishPostService {

    private final ContentPostRepository contentPostRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ContentPost createInProgressPost(Content content, ContentPlatform platform) {
        ContentPost post = ContentPost.builder()
                .content(content)
                .platform(platform)
                .status(PublishStatus.IN_PROGRESS)
                .build();
        return contentPostRepository.save(post);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completePost(UUID postId, String externalId, String postUrl, LocalDateTime publishedAt) {
        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        post.complete(externalId, postUrl, publishedAt);
        contentPostRepository.save(post);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failPost(UUID postId) {
        ContentPost post = contentPostRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        post.fail();
        contentPostRepository.save(post);
    }
}
