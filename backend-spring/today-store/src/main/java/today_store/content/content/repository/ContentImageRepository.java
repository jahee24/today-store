package today_store.content.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import today_store.content.content.entity.Content;
import today_store.content.content.entity.ContentImage;

import java.util.List;
import java.util.UUID;

public interface ContentImageRepository extends JpaRepository<ContentImage, UUID> {
    List<ContentImage> findByContentOrderByCreatedAtAsc(Content content);
    List<ContentImage> findByContentId(UUID uuid);
}
