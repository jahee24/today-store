package today_store.content.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import today_store.content.request.entity.GenerationRequest;
import today_store.content.request.entity.InputImage;

import java.util.List;
import java.util.UUID;

public interface InputImageRepository extends JpaRepository<InputImage, UUID> {
    List<InputImage> findByGenerationRequestOrderByDisplayOrderAsc(GenerationRequest generationRequest);
}
