package today_store.content.content.entity;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import today_store.content.request.entity.InputImage;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "input_image_variations")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InputImageVariation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_image_id", nullable = false)
    private InputImage inputImage;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "angle_type", length = 50)
    private String angleType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

