package today_store.content.content.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import today_store.content.request.entity.GenerationRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private GenerationRequest generationRequest;

    @Column(name = "instagram_text", columnDefinition = "TEXT")
    private String instagramText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instagram_hashtags", columnDefinition = "JSONB")
    private List<String> instagramHashtags = new ArrayList<>();

    @Column(name = "karrot_text", columnDefinition = "TEXT")
    private String karrotText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "karrot_tags", columnDefinition = "JSONB")
    private List<String> karrotTags = new ArrayList<>();

    @Column(name = "naver_text", columnDefinition = "TEXT")
    private String naverText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "naver_keywords", columnDefinition = "JSONB")
    private List<String> naverKeywords = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false, length = 20)
    private GenerationType generationType;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
