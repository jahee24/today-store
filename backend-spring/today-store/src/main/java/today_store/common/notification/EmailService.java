package today_store.common.notification;

public interface EmailService {
    void sendTaskNotification(String to, String subject, String content);
}
