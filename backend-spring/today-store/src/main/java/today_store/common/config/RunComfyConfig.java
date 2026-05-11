package today_store.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@Getter
public class RunComfyConfig {

    @Value("${app.runcomfy.api-key}")
    private String apiKey;

    @Value("${app.runcomfy.deployment-id}")
    private String deploymentId;

    @Value("${app.runcomfy.endpoint}")
    private String endpoint;

    @Value("${app.runcomfy.webhook-url}")
    private String webhookUrl;

    @Bean(name = "runComfyWebClient")
    public WebClient runComfyWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(120)) // ComfyUI can take longer
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)));

        return builder
                .baseUrl(endpoint)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
