package today_store.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import today_store.authentication.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileResponse {
    private UUID id;
    private LocalDateTime updatedAt;

    public static UpdateUserProfileResponse from(User user) {
        return UpdateUserProfileResponse.builder()
                .id(user.getId())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
