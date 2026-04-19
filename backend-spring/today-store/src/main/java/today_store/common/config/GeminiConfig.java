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
public class GeminiConfig {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Getter
    @Value("${app.gemini.model}")
    private String model;

    @Value("${app.gemini.endpoint}")
    private String endpoint;

    @Bean(name = "geminiWebClient")
    public WebClient geminiWebClient(WebClient.Builder builder) {
        // Gemini 전용 타임아웃 설정 (60초)
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 연결 타임아웃 10초
                .responseTimeout(Duration.ofSeconds(60))            // 응답 대기 60초
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public String getGenerateUrl() {
        return String.format("%s/%s:generateContent", endpoint, model);
    }

}
