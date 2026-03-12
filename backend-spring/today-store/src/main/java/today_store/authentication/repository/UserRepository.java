package today_store.authentication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import today_store.authentication.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}