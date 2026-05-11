package today_store.content.content.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import today_store.content.content.entity.ApiLog;
import today_store.content.content.entity.ApiStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiLogRepository extends JpaRepository<ApiLog, UUID> {
    List<ApiLog> findAllByStatusAndCreatedAtBefore(ApiStatus status, LocalDateTime dateTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ApiLog a WHERE a.id = :id")
    Optional<ApiLog> findByIdWithLock(@Param("id") UUID id);
}
