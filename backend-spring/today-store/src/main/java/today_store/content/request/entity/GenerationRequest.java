package today_store.content.request.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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

    @Column(name = "target_age", length = 50)
    private String targetAge;

    @Column(name = "target_gender", length = 20)
    private String targetGender;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(String concept, String additionalNote, String targetAge, String targetGender) {
        if (concept != null) this.concept = concept;
        if (additionalNote != null) this.additionalNote = additionalNote;
        if (targetAge != null) this.targetAge = targetAge;
        if (targetGender != null) this.targetGender = targetGender;
        updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}