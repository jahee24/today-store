package today_store.authentication.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import today_store.authentication.entity.RefreshToken;

import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {
}