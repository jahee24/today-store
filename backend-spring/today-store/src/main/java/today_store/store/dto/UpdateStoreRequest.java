package today_store.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.store.entity.PreferredStyle;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreRequest {
    private String storeName;
    private PreferredStyle preferredStyle;
    private String snsInstagram;
    private String snsNaverUrl;
    private String snsKarrotUrl;
    private String businessType;
    private String address;
}