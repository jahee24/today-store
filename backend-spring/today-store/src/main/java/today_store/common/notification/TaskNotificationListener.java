package today_store.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import today_store.content.content.event.TaskCompletedEvent;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskNotificationListener {

    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final NotificationMapper notificationMapper;
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

            String content = templateEngine.process("mail/task-notification", context);
            String subject = "[알림] " + translatedTaskType + " 작업이 완료되었습니다 (" + event.getStatus().name() + ")";

            emailService.sendTaskNotification(event.getUserEmail(), subject, content);
        } catch (Exception e) {
            log.error("Error processing email notification for apiLogId: {}", event.getApiLogId(), e);
        }
    }
}
