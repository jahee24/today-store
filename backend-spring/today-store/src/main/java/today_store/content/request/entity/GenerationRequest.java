package today_store.content.request.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import today_store.authentication.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "generation_requests")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GenerationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String concept;

    @Column(name = "additional_note", columnDefinition = "TEXT")
    private String additionalNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(String concept, String additionalNote) {
        if (concept != null) this.concept = concept;
        if (additionalNote != null) this.additionalNote = additionalNote;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}