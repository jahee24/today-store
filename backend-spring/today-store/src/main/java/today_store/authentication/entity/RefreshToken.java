package today_store.authentication.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.UUID;

@Getter
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 1296000)
public class RefreshToken {

    @Id
    private UUID userId;

    private String refreshToken;
}
