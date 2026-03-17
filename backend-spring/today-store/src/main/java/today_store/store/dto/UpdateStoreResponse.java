package today_store.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.store.entity.Store;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreResponse {
    private UUID id;
    private LocalDateTime updatedAt;

    public static UpdateStoreResponse from(Store store) {
        return UpdateStoreResponse.builder()
                .id(store.getId())
                .updatedAt(store.getUpdatedAt())
                .build();
    }
}
