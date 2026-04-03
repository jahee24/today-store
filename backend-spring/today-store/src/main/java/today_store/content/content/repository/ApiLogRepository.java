package today_store.content.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import today_store.content.content.entity.ApiLog;

import java.util.UUID;

public interface ApiLogRepository extends JpaRepository<ApiLog, UUID> {
}
