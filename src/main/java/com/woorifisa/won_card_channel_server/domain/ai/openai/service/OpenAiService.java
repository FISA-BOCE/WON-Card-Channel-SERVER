package com.woorifisa.won_card_channel_server.domain.ai.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.ai.openai.dto.request.OpenAiChatRequest;
import com.woorifisa.won_card_channel_server.domain.ai.openai.dto.request.OpenAiChatRequest.Message;
import com.woorifisa.won_card_channel_server.domain.ai.openai.dto.request.OpenAiChatRequest.ResponseFormat;
import com.woorifisa.won_card_channel_server.domain.ai.openai.dto.response.OpenAiChatResponse;
import com.woorifisa.won_card_channel_server.domain.ai.openai.exception.OpenAiErrorCode;
import com.woorifisa.won_card_channel_server.global.config.OpenAiConfig.OpenAiProperties;
import com.woorifisa.won_card_channel_server.global.config.QueryIntent;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final Duration openAiTimeout;

    private static final String CLASSIFY_SYSTEM_PROMPT =
            "You are an intent classifier for a Korean financial chatbot. " +
            "Analyze the user's question and respond ONLY with valid JSON in this exact format: " +
            "{\"intent\": \"<INTENT>\", \"confidence\": <0.0-1.0>, \"entities\": {<key>: <value>}} " +
            "Valid intents: TOTAL_SPEND, FOOD_SPEND, SHOPPING_SPEND, TRANSPORT_SPEND, " +
            "SUBSCRIPTION_SPEND, POINT_BALANCE, POINT_EARNED, REWARD_STATUS, " +
            "ETF_HOLDINGS, ETF_AMOUNT, MERCHANT_TO_ETF, POINT_BY_MERCHANT, SPEND_BY_CATEGORY, UNKNOWN.";

    private static final String GENERATE_SYSTEM_PROMPT =
            "You are a helpful Korean financial assistant. " +
            "Answer the user's question naturally in Korean using the provided data context. " +
            "Also suggest 2-3 follow-up questions the user might want to ask. " +
            "Respond ONLY with valid JSON: " +
            "{\"answer\": \"<answer>\", \"suggestedQuestions\": [\"<q1>\", \"<q2>\", \"<q3>\"]}";

    public ClassifyResult classifyIntent(String question) {
        String userPrompt = "질문: " + question;
        OpenAiChatRequest request = new OpenAiChatRequest(
                List.of(Message.system(CLASSIFY_SYSTEM_PROMPT), Message.user(userPrompt)),
                0.0,
                ResponseFormat.jsonObject()
        );

        String content = callOpenAi(request);
        return parseClassifyResult(content);
    }

    public GenerateResult generateResponse(String question, QueryIntent intent, String dataContext) {
        String userPrompt = buildGeneratePrompt(question, intent, dataContext);
        OpenAiChatRequest request = new OpenAiChatRequest(
                List.of(Message.system(GENERATE_SYSTEM_PROMPT), Message.user(userPrompt)),
                0.7,
                ResponseFormat.jsonObject()
        );

        String content = callOpenAi(request);
        return parseGenerateResult(content);
    }

    private String callOpenAi(OpenAiChatRequest request) {
        String url = String.format(
                "/openai/deployments/%s/chat/completions?api-version=%s",
                openAiProperties.getDeploymentName(),
                openAiProperties.getApiVersion()
        );

        try {
            OpenAiChatResponse response = openAiWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiChatResponse.class)
                    .timeout(openAiTimeout)
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new BusinessException(OpenAiErrorCode.OPENAI_API_ERROR);
            }
            return response.firstContent();

        } catch (WebClientRequestException e) {
            log.error("OpenAI request failed", e);
            throw new BusinessException(OpenAiErrorCode.OPENAI_TIMEOUT, e);
        } catch (WebClientResponseException e) {
            log.error("OpenAI response error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(OpenAiErrorCode.OPENAI_API_ERROR, e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.error("OpenAI timeout (block)", e);
                throw new BusinessException(OpenAiErrorCode.OPENAI_TIMEOUT, e);
            }
            throw e;
        }
    }

    private ClassifyResult parseClassifyResult(String content) {
        try {
            JsonNode node = objectMapper.readTree(content);
            String intentStr = node.path("intent").asText("UNKNOWN");
            double confidence = node.path("confidence").asDouble(0.0);

            QueryIntent intent;
            try {
                intent = QueryIntent.valueOf(intentStr);
            } catch (IllegalArgumentException e) {
                intent = QueryIntent.UNKNOWN;
            }

            JsonNode entitiesNode = node.path("entities");
            Map<String, String> entities = objectMapper.convertValue(entitiesNode, Map.class);

            return new ClassifyResult(intent, confidence, entities != null ? entities : Map.of());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse classify result: content_length={}", content != null ? content.length() : 0);
            return new ClassifyResult(QueryIntent.UNKNOWN, 0.0, Map.of());
        }
    }

    private GenerateResult parseGenerateResult(String content) {
        try {
            JsonNode node = objectMapper.readTree(content);
            String answer = node.path("answer").asText("");
            List<String> suggestedQuestions = objectMapper.convertValue(
                    node.path("suggestedQuestions"), List.class);
            return new GenerateResult(answer, suggestedQuestions != null ? suggestedQuestions : List.of());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse generate result: content_length={}", content != null ? content.length() : 0);
            return new GenerateResult(content, List.of());
        }
    }

    private String buildGeneratePrompt(String question, QueryIntent intent, String dataContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("사용자 질문: ").append(question).append("\n");
        sb.append("분류된 의도: ").append(intent.name()).append("\n");
        if (dataContext != null && !dataContext.isBlank()) {
            sb.append("조회 데이터:\n").append(dataContext);
        } else {
            sb.append("조회 데이터: 없음 (데이터 조회 실패 또는 해당 데이터 없음)");
        }
        return sb.toString();
    }

    public record ClassifyResult(QueryIntent intent, double confidence, Map<String, String> entities) {}

    public record GenerateResult(String answer, List<String> suggestedQuestions) {}
}
