package today_store.content.content.entity;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_posts")
@Getter
@Builder
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContentPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublishStatus status;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "post_url", length = 500)
    private String postUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public void complete(String postUrl) {
        this.status = PublishStatus.COMPLETED;
        this.postUrl = postUrl;
        this.publishedAt = LocalDateTime.now();
    }

    public void complete(String externalId, String postUrl, LocalDateTime publishedAt) {
        this.status = PublishStatus.COMPLETED;
        this.externalId = externalId;
        this.postUrl = postUrl;
        this.publishedAt = publishedAt;
    }

    public void fail() {
        this.status = PublishStatus.FAILED;
        this.postUrl = null;
        this.publishedAt = null;
    }
}
