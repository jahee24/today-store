package today_store.content.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import today_store.content.content.entity.InputImageVariation;
import today_store.content.request.entity.InputImage;

import java.util.List;
import java.util.UUID;

public interface InputImageVariationRepository extends JpaRepository<InputImageVariation, UUID> {
    List<InputImageVariation> findByInputImageOrderByCreatedAtAsc(InputImage inputImage);
    void deleteByInputImage(InputImage inputImage);
}
