package today_store.content.content.entity;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
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
}
