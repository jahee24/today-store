package today_store.common.gemini.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import today_store.common.config.GeminiConfig;
import today_store.common.gemini.dto.GeminiResponse;

@DisplayName("Gemini 서비스 테스트")
class GeminiServiceTest {

    private AtomicReference<ClientRequest> capturedRequest;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        capturedRequest = new AtomicReference<>();
        geminiService = new GeminiService(createGeminiConfig(), createWebClient(), new ObjectMapper());
    }

    @Test
    @DisplayName("Gemini 요청 헤더 전송")
    void shouldSendApiKeyViaHeaderWithoutQueryString() {
        // Gemini 요청을 보내면 API key를 URL query string이 아니라 x-goog-api-key 헤더로만 전달해야 한다.

        // given
        GeminiResponse response = geminiService.generateContent("prompt", List.of("https://signed.example.com/image.png")).block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getText()).isEqualTo("ok");
        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString())
                .isEqualTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                .doesNotContain("key=")
                .doesNotContain("test-secret-key");
        assertThat(capturedRequest.get().headers().getFirst("x-goog-api-key")).isEqualTo("test-secret-key");
    }

    private WebClient createWebClient() {
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {
                              "candidates": [
                                {
                                  "content": {
                                    "parts": [
                                      {"text": "ok"}
                                    ]
                                  }
                                }
                              ]
                            }
                            """)
                    .build());
        };

        return WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
    }

    private GeminiConfig createGeminiConfig() {
        GeminiConfig geminiConfig = new GeminiConfig();
        ReflectionTestUtils.setField(geminiConfig, "apiKey", "test-secret-key");
        ReflectionTestUtils.setField(geminiConfig, "model", "gemini-2.5-flash");
        ReflectionTestUtils.setField(geminiConfig, "endpoint", "https://generativelanguage.googleapis.com/v1beta/models");
        return geminiConfig;
    }
}
