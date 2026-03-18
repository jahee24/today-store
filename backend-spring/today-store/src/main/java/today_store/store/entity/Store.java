package today_store.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import today_store.authentication.entity.User;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ... other imports

@Entity
@Table(name = "stores")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;

    @Column(name = "business_type", nullable = false, length = 50)
    private String businessType;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_style", length = 50, nullable = false)
    @Builder.Default
    private PreferredStyle preferredStyle = PreferredStyle.CLEAN;

    @Column(name = "sns_instagram", length = 255)
    private String snsInstagram;

    @Column(name = "sns_naver_url", columnDefinition = "TEXT")
    private String snsNaverUrl;

    @Column(name = "sns_karrot_url", columnDefinition = "TEXT")
    private String snsKarrotUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String storeName, PreferredStyle preferredStyle, String snsInstagram, String snsNaverUrl, String snsKarrotUrl, String businessType, String address, BigDecimal latitude, BigDecimal longitude) {
        if (storeName != null) this.storeName = storeName;
        if (preferredStyle != null) this.preferredStyle = preferredStyle;
        if (snsInstagram != null) this.snsInstagram = snsInstagram;
        if (snsNaverUrl != null) this.snsNaverUrl = snsNaverUrl;
        if (snsKarrotUrl != null) this.snsKarrotUrl = snsKarrotUrl;
        if (businessType != null) this.businessType = businessType;
        if (address != null) this.address = address;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
    }
}
