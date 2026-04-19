package today_store.content.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentPlatform;
import today_store.content.content.entity.ContentPost;
import today_store.content.content.entity.PublishStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPostRepository extends JpaRepository<ContentPost, UUID> {
    boolean existsByContentAndStatus(Content content, PublishStatus status);

    Optional<ContentPost> findFirstByContentAndPlatformAndStatusOrderByPublishedAtDesc(Content content, ContentPlatform platform, PublishStatus status);

    @Query("SELECT cp.content.id FROM ContentPost cp WHERE cp.content IN :contents AND cp.status = :status")
    List<UUID> findContentIdsByContentInAndStatus(Collection<Content> contents, PublishStatus status);
}
