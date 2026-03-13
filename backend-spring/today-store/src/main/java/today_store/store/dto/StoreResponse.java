package today_store.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.store.entity.PreferredStyle;

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
}