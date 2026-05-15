package today_store.common.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfig {

    @Bean
    public Resend resend(@Value("${app.resend.api-key}") String apiKey) {
        return new Resend(apiKey);
    }
}
