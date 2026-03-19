package today_store.content.request.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import today_store.authentication.entity.User;
import today_store.content.request.entity.GenerationRequest;

import java.util.Optional;
import java.util.UUID;

public interface GenerationRequestRepository extends JpaRepository<GenerationRequest, UUID> {
    Page<GenerationRequest> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user, Pageable pageable);
    Optional<GenerationRequest> findByIdAndIsDeletedFalse(UUID id);
}
