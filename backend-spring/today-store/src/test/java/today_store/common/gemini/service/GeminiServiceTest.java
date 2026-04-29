package today_store.common.gemini.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
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
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gemini.dto.GeminiParsedResponse;
import today_store.common.gemini.dto.GeminiRegenerationRequest;
import today_store.common.gemini.dto.GeminiResponse;

@DisplayName("Gemini 서비스 테스트")
class GeminiServiceTest {

    private AtomicReference<ClientRequest> capturedRequest;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        capturedRequest = new AtomicReference<>();
        geminiService = new GeminiService(
                createGeminiConfig(),
                createWebClient(jsonResponse(HttpStatus.OK, """
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "ok"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """)),
                new ObjectMapper()
        );
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

    @Test
    @DisplayName("Gemini 응답 fenced JSON 파싱")
    void shouldParseFencedJsonAndCopyMetadata() {
        // fenced JSON 응답은 정상 파싱하고 usage metadata를 결과에 복사해야 한다.

        // given
        GeminiResponse response = responseWithText("""
                ```json
                {
                  "photo_info": "Sunny cafe",
                  "text": "Body copy",
                  "hashtags": ["cafe"]
                }
                ```
                """);
        response.setUsageMetadata(new GeminiResponse.UsageMetadata(120, 45, 165));
        response.setResponseTimeMs(987L);

        // when
        GeminiParsedResponse parsed = geminiService.processResponse(response);

        // then
        assertThat(parsed.getPhotoInfo()).isEqualTo("Sunny cafe");
        assertThat(parsed.getText()).isEqualTo("Body copy");
        assertThat(parsed.getHashtags()).containsExactly("#cafe");
        assertThat(parsed.getInputTokens()).isEqualTo(120);
        assertThat(parsed.getOutputTokens()).isEqualTo(45);
        assertThat(parsed.getResponseTimeMs()).isEqualTo(987L);
    }

    @Test
    @DisplayName("Gemini 해시태그 정규화")
    void shouldNormalizeHashtagsAndRemoveTail() {
        // 본문 마지막 줄 해시태그는 제거하고 hashtags는 중복 없이 정규화해야 한다.

        // given
        GeminiResponse response = responseWithText("""
                {
                  "photo_info": "Cozy cafe",
                  "text": "Fresh coffee is ready.\\n#spring #cafe #spring",
                  "hashtags": [" spring", "#cafe", "", null, "spring"]
                }
                """);

        // when
        GeminiParsedResponse parsed = geminiService.processResponse(response);

        // then
        assertThat(parsed.getText()).isEqualTo("Fresh coffee is ready.");
        assertThat(parsed.getHashtags()).containsExactly("#spring", "#cafe");
    }

    @Test
    @DisplayName("Gemini 본문 줄바꿈 정리")
    void shouldSoftWrapLongSingleLineText() {
        // 단일 줄의 여러 문장은 문장 기준으로 줄바꿈을 추가해 가독성을 높여야 한다.

        // given
        GeminiResponse response = responseWithText("""
                {
                  "photo_info": "Cafe",
                  "text": "First sentence. Second sentence! Third sentence?",
                  "hashtags": []
                }
                """);

        // when
        GeminiParsedResponse parsed = geminiService.processResponse(response);

        // then
        assertThat(parsed.getText()).isEqualTo("First sentence.\nSecond sentence!\nThird sentence?");
    }

    @Test
    @DisplayName("Gemini 빈 응답 예외")
    void shouldThrowWhenResponseTextIsEmpty() {
        // Gemini 응답 본문이 비어 있으면 AI001 예외를 반환해야 한다.

        // given
        GeminiResponse response = GeminiResponse.builder().candidates(List.of()).build();

        // when
        CustomException exception = assertThrows(CustomException.class, () -> geminiService.processResponse(response));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_EMPTY);
    }

    @Test
    @DisplayName("Gemini JSON 파싱 예외")
    void shouldThrowWhenJsonParsingFails() {
        // 응답 JSON 파싱에 실패하면 AI002 예외를 반환해야 한다.

        // given
        GeminiResponse response = responseWithText("not-json");

        // when
        CustomException exception = assertThrows(CustomException.class, () -> geminiService.processResponse(response));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_PARSE_ERROR);
    }

    @Test
    @DisplayName("Gemini 재생성 응답 후처리")
    void shouldProcessRegenerationContent() {
        // 재생성 응답도 동일한 후처리 파이프라인을 거쳐 본문과 해시태그를 정리해야 한다.

        // given
        geminiService = new GeminiService(createGeminiConfig(), createWebClient(jsonResponse(HttpStatus.OK, """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"text\\":\\"Refined copy.\\\\n#fresh\\",\\"hashtags\\":[\\"fresh\\"]}"
                          }
                        ]
                      }
                    }
                  ],
                  "usageMetadata": {
                    "promptTokenCount": 210,
                    "candidatesTokenCount": 80,
                    "totalTokenCount": 290
                  }
                }
                """)), new ObjectMapper());

        // when
        GeminiParsedResponse parsed = geminiService.generateProcessedRegenerationContent(
                GeminiRegenerationRequest.builder()
                        .originalText("Old text")
                        .originalHashtags(List.of("#old"))
                        .feedback("Make it sharper")
                        .target("INSTAGRAM")
                        .build()
        ).block();

        // then
        assertThat(parsed).isNotNull();
        assertThat(parsed.getText()).isEqualTo("Refined copy.");
        assertThat(parsed.getHashtags()).containsExactly("#fresh");
        assertThat(parsed.getInputTokens()).isEqualTo(210);
        assertThat(parsed.getOutputTokens()).isEqualTo(80);
    }

    private GeminiResponse responseWithText(String text) {
        return GeminiResponse.builder()
                .candidates(List.of(
                        new GeminiResponse.Candidate(
                                new GeminiResponse.Content(
                                        List.of(new GeminiResponse.Part(text)),
                                        "model"
                                ),
                                "STOP"
                        )
                ))
                .build();
    }

    private WebClient createWebClient(ClientResponse... responses) {
        Queue<ClientResponse> queue = new ArrayDeque<>(List.of(responses));
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            ClientResponse response = queue.poll();
            if (response == null) {
                return Mono.error(new IllegalStateException("No stubbed response for request: " + request.url()));
            }
            return Mono.just(response);
        };

        return WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
    }

    private ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
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
