package today_store.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "소셜 로그인 제공자(provider)는 필수 항목입니다.")
    private String provider;
    
    @NotBlank(message = "액세스 토큰(accessToken)은 필수 항목입니다.")
    private String accessToken;
}
