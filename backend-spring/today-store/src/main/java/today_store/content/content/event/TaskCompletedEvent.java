package today_store.content.content.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import today_store.content.content.entity.ApiLog;
import today_store.content.content.entity.ApiStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class TaskCompletedEvent extends ApplicationEvent {
    private final UUID apiLogId;
    private final String userEmail;
    private final ApiStatus status;
    private final String taskType;
    private final String errorMessage;
    private final LocalDateTime submittedAt;
    private final List<String> imageUrls;

    public TaskCompletedEvent(Object source, ApiLog apiLog) {
        this(source, apiLog, Collections.emptyList());
    }

    public TaskCompletedEvent(Object source, ApiLog apiLog, List<String> imageUrls) {
        super(source);
        this.apiLogId = apiLog.getId();
        this.userEmail = apiLog.getGenerationRequest().getUser().getEmail();
        this.status = apiLog.getStatus();
        this.taskType = apiLog.getModel();
        this.errorMessage = apiLog.getErrorMessage();
        this.submittedAt = apiLog.getCreatedAt();
        this.imageUrls = imageUrls != null ? imageUrls : Collections.emptyList();
    }
}