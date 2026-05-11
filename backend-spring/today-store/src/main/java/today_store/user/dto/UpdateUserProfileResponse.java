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
    private String email;
    private String name;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime updatedAt;

    public static UpdateUserProfileResponse from(User user) {
        return from(user, null, null);
    }

    public static UpdateUserProfileResponse from(User user, String accessToken, String refreshToken) {
        return UpdateUserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}