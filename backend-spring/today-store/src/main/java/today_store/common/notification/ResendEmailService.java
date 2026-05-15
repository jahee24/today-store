package today_store.common.notification;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class ResendEmailService implements EmailService {

    private final Resend resend;
    private final String fromEmail;

    public ResendEmailService(Resend resend, @Value("${app.resend.from-email}") String fromEmail) {
        this.resend = resend;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendTaskNotification(String to, String subject, String htmlContent) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(htmlContent)
                .build();

        Mono.fromCallable(() -> {
                    log.info("Attempting to send email via Resend to {}", to);
                    return resend.emails().send(params);
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof ResendException)
                        .doBeforeRetry(retrySignal -> log.warn("Retrying email sending to {}... Attempt: {}", to, retrySignal.totalRetries() + 1))
                )
                .doOnSuccess(response -> log.info("Email sent successfully via Resend. ID: {}", response.getId()))
                .doOnError(e -> log.error("Failed to send email via Resend to {} after all attempts. Error: {}", to, e.getMessage(), e))
                .onErrorResume(e -> Mono.empty()) // Swallow exception to prevent affecting main flow
                .block();
    }
}