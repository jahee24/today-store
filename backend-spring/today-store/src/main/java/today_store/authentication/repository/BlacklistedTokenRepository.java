package today_store.authentication.repository;

import org.springframework.data.repository.CrudRepository;
import today_store.authentication.entity.BlacklistedToken;

public interface BlacklistedTokenRepository extends CrudRepository<BlacklistedToken, String> {
}