package today_store.authentication.repository;

import org.springframework.data.repository.CrudRepository;
import today_store.authentication.entity.RefreshToken;

import java.util.UUID;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {
}