package today_store.content.content.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import today_store.content.content.entity.Content;
import today_store.content.request.entity.GenerationRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {
    List<Content> findByGenerationRequestAndIsDeletedFalseOrderByCreatedAtDesc(GenerationRequest generationRequest);
    Optional<Content> findByIdAndIsDeletedFalse(UUID id);
}
