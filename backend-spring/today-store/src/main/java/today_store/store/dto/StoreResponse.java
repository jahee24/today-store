package today_store.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.store.entity.PreferredStyle;
import today_store.store.entity.Store;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    private UUID id;
    private String storeName;
    private String businessType;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private PreferredStyle preferredStyle;
    private SnsInfo sns;

    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .storeName(store.getStoreName())
                .businessType(store.getBusinessType())
                .address(store.getAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .preferredStyle(store.getPreferredStyle())
                .sns(SnsInfo.builder()
                        .instagram(store.getSnsInstagram())
                        .naver(store.getSnsNaverUrl())
                        .karrot(store.getSnsKarrotUrl())
                        .build())
                .build();
    }
}