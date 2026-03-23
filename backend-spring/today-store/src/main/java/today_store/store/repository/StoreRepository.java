package today_store.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import today_store.authentication.entity.User;
import today_store.store.entity.Store;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    Optional<Store> findByUser(User user);
    boolean existsByUser(User user);
}