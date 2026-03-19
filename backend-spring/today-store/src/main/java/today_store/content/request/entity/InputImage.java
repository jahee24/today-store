package today_store.content.request.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "input_images")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InputImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private GenerationRequest generationRequest;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void update(String description, Integer displayOrder) {
        if (description != null) this.description = description;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }
}