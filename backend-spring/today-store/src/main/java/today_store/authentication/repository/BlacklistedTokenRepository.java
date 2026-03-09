package today_store.authentication.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import today_store.authentication.entity.BlacklistedToken;

@Repository
public interface BlacklistedTokenRepository extends CrudRepository<BlacklistedToken, String> {
}