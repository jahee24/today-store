package today_store.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import today_store.common.gcs.GcsService;
import today_store.content.content.event.TaskCompletedEvent;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskNotificationListener {

    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final NotificationMapper notificationMapper;
    private final GcsService gcsService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskCompletedEvent(TaskCompletedEvent event) {
        log.info("Handling TaskCompletedEvent for apiLogId: {}, status: {}", event.getApiLogId(), event.getStatus());

        try {
            String translatedTaskType = notificationMapper.translateTaskType(event.getTaskType());
            String translatedErrorMessage = notificationMapper.translateErrorMessage(event.getErrorMessage());

            Context context = new Context();
            context.setVariable("taskType", translatedTaskType);
            context.setVariable("status", event.getStatus().name());
            context.setVariable("errorMessage", translatedErrorMessage);
            context.setVariable("submittedAt", event.getSubmittedAt().format(FORMATTER));

            // Convert GCS object keys to 7‑day signed URLs for email rendering
            List<String> signedUrls = event.getImageUrls().stream()
                    .map(url -> gcsService.generateSignedUrl(url, 7, TimeUnit.DAYS))
                    .toList();
            context.setVariable("resultImageUrls", signedUrls);

            String content = templateEngine.process("mail/task-notification", context);
            String subject = "[알림] " + translatedTaskType + " 작업이 완료되었습니다 (" + event.getStatus().name() + ")";

            emailService.sendTaskNotification(event.getUserEmail(), subject, content);
        } catch (Exception e) {
            log.error("Error processing email notification for apiLogId: {}", event.getApiLogId(), e);
        }
    }
}
