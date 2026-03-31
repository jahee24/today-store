package today_store.common.gemini.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import today_store.common.config.GeminiConfig;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.gemini.dto.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiService(GeminiConfig geminiConfig,
                         @Qualifier("geminiWebClient") WebClient webClient,
                         ObjectMapper objectMapper) {
        this.geminiConfig = geminiConfig;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public Mono<GeminiResponse> generateContent(String prompt, List<String> signedUrls) {
        String url = geminiConfig.getGenerateUrl();

        List<GeminiGenerationRequest.Part> parts = new ArrayList<>();
        parts.add(GeminiGenerationRequest.Part.builder().text(prompt).build());

        if (signedUrls != null) {
            for (String signedUrl : signedUrls) {
                parts.add(GeminiGenerationRequest.Part.builder()
                        .fileData(GeminiGenerationRequest.FileData.builder()
                                .mimeType("image/jpeg")
                                .fileUri(signedUrl)
                                .build())
                        .build());
            }
        }

        GeminiGenerationRequest request = GeminiGenerationRequest.builder()
                .contents(List.of(GeminiGenerationRequest.Content.builder().parts(parts).build()))
                .build();

        log.info("Calling Gemini API with Dedicated WebClient: {}", geminiConfig.getModel());

        long startTime = System.currentTimeMillis();
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(response -> {
                    // response time 직접 계산
                    response.setResponseTimeMs(System.currentTimeMillis() - startTime);
                    return response;
                });
    }

    // 프롬프트 적용
    public Mono<GeminiResponse> generateContent(GeminiPromptRequest request) {

        String basePrompt = """
                        ## ROLE
                        당신은 소상공인을 위한 전문 SNS 마케팅 카피라이터 '오늘의 가게(Today's Store)' AI 엔진입니다.
                        사용자가 업로드한 사진을 분석하고, 매장의 특성을 살린 매력적인 홍보 문구를 생성하는 역할을 수행합니다.
                                        
                        ## DATA DEFINITION: photo_info
                        - 정의: AI가 업로드된 이미지를 보고 직접 분석한 시각적 정보의 요약입니다.
                        - 역할: 생성된 문구의 직접적인 근거가 되며, 응답 품질 점검 시 디버깅 데이터로 활용됩니다.
                                        
                        ## STYLE GUIDELINES
                        1. 감성적: 따뜻하고 스토리텔링 중심의 톤, 감성적 형용사와 이모지 활용.
                        2. 정보제공: 객관적이고 상세한 정보 전달, 가격/스펙/구성 위주.
                        3. 전문성: 신뢰감 있는 전문적 톤, 브랜드 가치 강조, 전문 용어 활용.
                        4. 친근: 이웃 같은 편안한 대화 톤, 구어체 및 공감 유도.
                                        
                        ## SUCCESS CRITERIA
                        1. 이미지를 분석해 photo_info를 작성합니다.
                        2. 입력된 조건을 반영해 마케팅 문구를 작성합니다.
                        3. 반드시 JSON 객체만 출력합니다.
                                        
                        ## OUTPUT GUARDRAIL
                        - "본문"에는 해시태그(#...)를 절대 포함하지 마세요.
                        - 해시태그는 반드시 "해시태그" 배열에만 넣으세요.
                        - 본문 마지막 줄에 해시태그를 반복 출력하지 마세요.
                """;

        StringBuilder promptBuilder = new StringBuilder();

        List<String> descriptions = request.getImageDescriptions();
        String imageDescriptionsStr;
        if (descriptions == null || descriptions.isEmpty()) {
            imageDescriptionsStr = "제공되지 않음";
        } else {
            List<String> indexedList = new ArrayList<>();
            for (int i = 0; i < descriptions.size(); i++) {
                String desc = (descriptions.get(i) != null && !descriptions.get(i).isBlank())
                        ? descriptions.get(i)
                        : "설명 없음";
                indexedList.add(String.format("[사진 %d]: %s", i + 1, desc));
            }
            imageDescriptionsStr = String.join(", ", indexedList);
        }

        promptBuilder
                .append(basePrompt)
                .append(String.format("""
                        ## USER INPUT
                        - store_name: %s
                        - business_type: %s
                        - address: %s
                        - lat: %s
                        - lng: %s
                        - target_age : %s
                        - target_gender : %s
                        - style: %s
                        - additional_note: %s
                        - image_descriptions : %s
                        - image_count: %d (최대 5)
                        """,
                        request.getStoreName(),
                        request.getBusinessType(),
                        request.getAddress(),
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getTargetAge(),
                        request.getTargetGender(),
                        request.getConcept(),
                        request.getAdditionalNote(),
                        imageDescriptionsStr,
                        request.getSignedUrls() != null ? request.getSignedUrls().size() : 0
                ))
                .append(
                        """
                        ## IMPORTANT RULES
                        1. 상호명은 반드시 store_name만 사용하세요. 이미지에서 읽힌 텍스트를 상호명으로 추정하지 마세요.
                        2. 이미지 텍스트(OCR)는 메뉴/분위기/특징 보조 정보로만 사용하세요.
                        3. 본문은 가독성 높은 2~4문장으로 작성하고, 한 줄 장문으로 뭉치지 않게 작성하세요.
                        4. 문장 간 자연스러운 줄바꿈을 1~2회 허용하세요.
                        5. 반드시 JSON 객체만 출력하세요. 코드펜스/설명문은 금지합니다.
                        6. JSON 키는 정확히 다음 3개만 사용하세요: photo_info, text, hashtags

                        ## FEW-SHOT (좋은 예)
                        입력 조건:
                        - store_name: 홍길동 카페
                        - style: 친근함

                        출력 예:
                        {
                          "photo_info": "우드톤 인테리어, 라떼 아트, 디저트 진열대가 보이는 아늑한 카페 공간",
                          "text": "오늘은 한 템포 쉬어가고 싶은 날, 홍길동 카페에서 여유를 채워보세요. \n 부드러운 라떼와 달콤한 디저트로 일상에 작은 기분전환을 더해드립니다.",
                          "hashtags": ["#카페", "#디저트", "#라떼"]
                        }

                                                
                        ## FEW-SHOT (나쁜 예: 금지)
                        - 한 줄에 과도하게 긴 문장만 작성
                        - 이미지에서 본 텍스트를 store_name으로 바꿔 부르는 행위
                        - 본문 끝에 해시태그 나열
                                                
                        ## OUTPUT JSON STRUCTURE
                        {
                          "photo_info": "AI의 시각적 분석 결과",
                          "text": "생성된 마케팅 문구",
                          "hashtags": ["#태그1", "#태그2", "#태그3"]
                        }
                        """
                );



        return generateContent(promptBuilder.toString(), request.getSignedUrls());
    }


    public Mono<GeminiParsedResponse> generateProcessedContent(GeminiPromptRequest request) {
        return generateContent(request)
                .map(this::processResponse);
    }

    // 임시로 적용한 재생성 프롬프트(수정 예정)
    public Mono<GeminiParsedResponse> generateProcessedRegenerationContent(GeminiRegenerationRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 전문 마케팅 AI 에이전트입니다. 다음 정보를 바탕으로 홍보 문구와 태그를 재작성해주세요.\n\n");
        promptBuilder.append("### 기존 문구:\n");
        promptBuilder.append("- 인스타그램: ").append(request.getOriginalInstagramText()).append("\n");
        promptBuilder.append("- 당근마켓: ").append(request.getOriginalKarrotText()).append("\n");
        promptBuilder.append("- 네이버: ").append(request.getOriginalNaverText()).append("\n\n");

        promptBuilder.append("### 사용자 피드백: ").append(request.getFeedback()).append("\n");

        promptBuilder.append("응답은 반드시 아래 JSON 형식을 지켜주세요:\n");
        promptBuilder.append("""
                            {
                                "photo_info": "AI의 시각적 분석 결과",
                                "text": "생성된 마케팅 문구",
                                "hashtags": ["#태그1", "#태그2", "#태그3"]
                            }
                """);

        return generateContent(promptBuilder.toString(), null)
                .map(this::processResponse);
    }


    public GeminiParsedResponse processResponse(GeminiResponse response) {
        String text = response.getText();
        if (text == null) {
            throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
        }

        GeminiParsedResponse parsed = parseGeminiResponse(text);

        // 후처리 적용
        List<String> normalizedHashtags = normalizeHashtags(parsed.getHashtags());
        String cleanText = removeHashtagTailFromBody(parsed.getText(), normalizedHashtags);
        cleanText = softWrapLongText(cleanText);

        parsed.setHashtags(normalizedHashtags);
        parsed.setText(cleanText);

        // 메타데이터 복사
        if (response.getUsageMetadata() != null) {
            parsed.setInputTokens(response.getUsageMetadata().getPromptTokenCount());
            parsed.setOutputTokens(response.getUsageMetadata().getCandidatesTokenCount());
        }
        parsed.setResponseTimeMs(response.getResponseTimeMs());

        return parsed;
    }

    private GeminiParsedResponse parseGeminiResponse(String text) {
        String cleanJson = text.replaceAll("```json|```", "").trim();
        try {
            return objectMapper.readValue(cleanJson, GeminiParsedResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response: {}", text);
            throw new CustomException(ErrorCode.AI_PARSE_ERROR);
        }
    }

    // 가드레일 1
    private List<String> normalizeHashtags(List<String> hashtags) {
        if (hashtags == null) return Collections.emptyList();

        return hashtags.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .map(s -> s.startsWith("#") ? s : "#" + s)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }


    // 가드레일 2
    private String removeHashtagTailFromBody(String body, List<String> hashtags) {
        if (body == null || body.trim().isEmpty()) return "";
        if (hashtags == null || hashtags.isEmpty()) return body.trim();

        String[] lines = body.split("\\r?\\n");
        if (lines.length == 0) return body.trim();

        String lastLine = lines[lines.length - 1].trim();
        if (lastLine.isEmpty()) return body.trim();

        Pattern hashtagPattern = Pattern.compile("#[^\\s#]+");
        var matcher = hashtagPattern.matcher(lastLine);

        List<String> lastLineTokens = new ArrayList<>();
        while (matcher.find()) {
            lastLineTokens.add(matcher.group());
        }

        if (!lastLineTokens.isEmpty()) {
            boolean allAreHashtags = lastLineTokens.stream().allMatch(hashtags::contains);
            String withoutHashtags = lastLine.replaceAll("#[^\\s#]+", "").trim();

            if (allAreHashtags && withoutHashtags.isEmpty()) {
                return Arrays.stream(lines, 0, lines.length - 1)
                        .collect(Collectors.joining("\n")).trim();
            }
        }

        return body.trim();
    }

    // 가드레일 3
    private String softWrapLongText(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (text.contains("\n")) return text.trim();

        String[] chunks = cleaned.split("(?<=[.!?])\\s+");
        if (chunks.length >= 2) {
            return Arrays.stream(chunks)
                    .limit(4)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("\n"));
        }
        return cleaned;
    }
}
