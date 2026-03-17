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
public class CreateStoreResponse {
    private UUID id;
    private LocalDateTime createdAt;

    public static CreateStoreResponse from(Store store) {
        return CreateStoreResponse.builder()
                .id(store.getId())
                .createdAt(store.getCreatedAt())
                .build();
    }
}