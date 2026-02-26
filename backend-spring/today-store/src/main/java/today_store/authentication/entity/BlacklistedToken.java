package today_store.authentication.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@AllArgsConstructor
@RedisHash(value = "blacklistedToken")
public class BlacklistedToken {

    @Id
    private String token;

    private String status;

    @TimeToLive
    private Long timeToLive;
}