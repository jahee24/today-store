package today_store.content.content.entity;

import jakarta.persistence.*;
import lombok.*;
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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Builder.Default
    private List<String> instagramHashtags = new ArrayList<>();

    @Column(name = "karrot_text", columnDefinition = "TEXT")
    private String karrotText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "karrot_tags", columnDefinition = "JSONB")
    @Builder.Default
    private List<String> karrotTags = new ArrayList<>();

    @Column(name = "naver_text", columnDefinition = "TEXT")
    private String naverText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "naver_keywords", columnDefinition = "JSONB")
    @Builder.Default
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

    public void updateInstagram(String text, List<String> hashtags) {
        if (text != null) this.instagramText = text;
        if (hashtags != null) this.instagramHashtags = hashtags;
    }

    public void updateKarrot(String text, List<String> tags) {
        if (text != null) this.karrotText = text;
        if (tags != null) this.karrotTags = tags;
    }

    public void updateNaver(String text, List<String> keywords) {
        if (text != null) this.naverText = text;
        if (keywords != null) this.naverKeywords = keywords;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
