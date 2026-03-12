package today_store.authentication.dto;

import lombok.Builder;
import lombok.Data;
import today_store.authentication.entity.User;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserDetails user;

    @Data
    @Builder
    public static class UserDetails {
        private UUID id;
        private String email;
        private String name;
        private boolean isFirstLogin;

        public static UserDetails from(User user, boolean isFirstLogin) {
            return UserDetails.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .isFirstLogin(isFirstLogin)
                    .build();
        }
    }
}