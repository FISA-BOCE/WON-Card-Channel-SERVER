package com.woorifisa.won_card_channel_server.domain.ai.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.ai.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.global.util.PiiTextMasker;
import com.woorifisa.won_card_channel_server.domain.ai.invest.model.InvestSummary;
import com.woorifisa.won_card_channel_server.domain.ai.invest.repository.InvestSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.ai.openai.service.OpenAiService;
import com.woorifisa.won_card_channel_server.domain.ai.openai.service.OpenAiService.ClassifyResult;
import com.woorifisa.won_card_channel_server.domain.ai.openai.service.OpenAiService.GenerateResult;
import com.woorifisa.won_card_channel_server.domain.ai.spend.model.SpendSummary;
import com.woorifisa.won_card_channel_server.domain.ai.spend.repository.SpendSummaryRepository;
import com.woorifisa.won_card_channel_server.global.config.QueryIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiService openAiService;
    private final SpendSummaryRepository spendSummaryRepository;
    private final InvestSummaryRepository investSummaryRepository;
    private final ObjectMapper objectMapper;

    @Qualifier("cardNeo4jDriver")
    private final Driver cardNeo4jDriver;

    @Qualifier("securitiesNeo4jDriver")
    private final Driver securitiesNeo4jDriver;

    private static final double CONFIDENCE_THRESHOLD = 0.7;
    private static final String LOW_CONFIDENCE_ANSWER =
            "질문을 더 구체적으로 입력해 주세요. 예: '이번 달 식비가 얼마야?', '내 ETF 수익률 알려줘'";

    public ChatResponse processChat(String question, UUID userUuid) {
        String sanitizedQuestion = PiiTextMasker.mask(question);

        // 1. 의도 분류
        ClassifyResult classifyResult = openAiService.classifyIntent(sanitizedQuestion);
        log.info("intent={}, confidence={}", classifyResult.intent(), classifyResult.confidence());

        // 2. confidence 낮으면 재질문 유도
        if (classifyResult.confidence() < CONFIDENCE_THRESHOLD) {
            return new ChatResponse(LOW_CONFIDENCE_ANSWER, List.of(), List.of());
        }

        // 3-4. DB 조회 (실패해도 partial 응답 허용)
        List<String> contextUsed = new ArrayList<>();
        String dataContext = fetchDataContext(classifyResult, userUuid, contextUsed);

        // 5-6. 자연어 답변 생성
        GenerateResult generateResult = openAiService.generateResponse(
                sanitizedQuestion, classifyResult.intent(), dataContext);

        return new ChatResponse(generateResult.answer(), contextUsed, generateResult.suggestedQuestions());
    }

    private String fetchDataContext(ClassifyResult classifyResult, UUID userUuid, List<String> contextUsed) {
        QueryIntent intent = classifyResult.intent();

        if (isSecuritiesMysqlIntent(intent)) {
            return fetchEtfContext(intent, userUuid, contextUsed);
        }

        try {
            if (isCardMysqlIntent(intent)) {
                Optional<SpendSummary> summary = spendSummaryRepository.findLatestByUserUuid(userUuid);
                if (summary.isPresent()) {
                    contextUsed.add("CARD_TRANSACTION");
                    return buildSpendContext(summary.get(), intent);
                }
            } else if (isCardNeo4jIntent(intent)) {
                String result = queryCardNeo4j(intent, userUuid);
                if (result != null) {
                    contextUsed.add("CARD_GRAPH");
                    return result;
                }
            } else if (intent == QueryIntent.MERCHANT_TO_ETF) {
                String result = queryMerchantToEtf(userUuid);
                if (result != null) {
                    contextUsed.add("CARD_GRAPH");
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("DB query failed for intent={}, userUuid={}: {}", intent, userUuid, e.getMessage());
            return "";
        }

        return null;
    }

    private String fetchEtfContext(QueryIntent intent, UUID userUuid, List<String> contextUsed) {
        List<String> parts = new ArrayList<>();
        boolean hadException = false;

        try {
            Optional<InvestSummary> summary = investSummaryRepository.findByUserUuid(userUuid);
            if (summary.isPresent()) {
                contextUsed.add("INVESTMENT_DATA");
                parts.add(buildInvestContext(summary.get(), intent));
            }
        } catch (Exception e) {
            hadException = true;
            log.warn("InvestSummary query failed for intent={}, userUuid={}: {}", intent, userUuid, e.getMessage());
        }

        try {
            String neo4jResult = querySecuritiesNeo4j(intent, userUuid);
            if (neo4jResult != null) {
                contextUsed.add("SECURITIES_GRAPH");
                parts.add(neo4jResult);
            }
        } catch (Exception e) {
            hadException = true;
            log.warn("Securities Neo4j query failed for intent={}, userUuid={}: {}", intent, userUuid, e.getMessage());
        }

        if (parts.isEmpty()) {
            return hadException ? "" : null;
        }
        return String.join("\n", parts);
    }

    private boolean isCardMysqlIntent(QueryIntent intent) {
        return switch (intent) {
            case TOTAL_SPEND, FOOD_SPEND, SHOPPING_SPEND, TRANSPORT_SPEND,
                 SUBSCRIPTION_SPEND, POINT_BALANCE, POINT_EARNED, REWARD_STATUS -> true;
            default -> false;
        };
    }

    private boolean isSecuritiesMysqlIntent(QueryIntent intent) {
        return intent == QueryIntent.ETF_HOLDINGS || intent == QueryIntent.ETF_AMOUNT;
    }

    private boolean isCardNeo4jIntent(QueryIntent intent) {
        return intent == QueryIntent.POINT_BY_MERCHANT || intent == QueryIntent.SPEND_BY_CATEGORY;
    }

    private String buildSpendContext(SpendSummary summary, QueryIntent intent) {
        return switch (intent) {
            case TOTAL_SPEND -> "총 지출: " + summary.getTotalSpendAmount() + "원 (기준월: " + summary.getBaseMonth() + ")";
            case FOOD_SPEND -> "음식 지출: " + summary.getFoodAmount() + "원";
            case SHOPPING_SPEND -> "쇼핑 지출: " + summary.getShoppingAmount() + "원";
            case TRANSPORT_SPEND -> "교통 지출: " + summary.getTransportAmount() + "원";
            case SUBSCRIPTION_SPEND -> "구독 지출: " + summary.getSubscriptionAmount() + "원";
            case POINT_BALANCE -> "포인트 잔액: " + summary.getPointBalanceAmount() + "P";
            case POINT_EARNED -> "당월 적립 기준 지출: " + summary.getCurrentMonthSpendAmount() + "원";
            case REWARD_STATUS -> "리워드율: " + summary.getRewardRate() + "%, 달성 상태: " + summary.getPerformanceStatus();
            default -> toJson(summary);
        };
    }

    private String buildInvestContext(InvestSummary summary, QueryIntent intent) {
        return switch (intent) {
            case ETF_HOLDINGS -> "ETF 보유 현황: " + summary.getEtfSummaryJson();
            case ETF_AMOUNT -> "총 매수금액: " + summary.getTotalBuyAmount() + "원, 손익: " + summary.getProfitLossAmount() + "원";
            default -> toJson(summary);
        };
    }

    private String queryCardNeo4j(QueryIntent intent, UUID userUuid) {
        String cypher = switch (intent) {
            case POINT_BY_MERCHANT ->
                    "MATCH (p:Payment)-[:EARNED]->(pt:Point) " +
                    "WHERE p.userUuid = $userUuid " +
                    "RETURN p.merchant AS merchant, sum(pt.amount) AS totalPoints " +
                    "ORDER BY totalPoints DESC LIMIT 10";
            case SPEND_BY_CATEGORY ->
                    "MATCH (p:Payment)-[:BELONGS_TO]->(c:Category) " +
                    "WHERE p.userUuid = $userUuid " +
                    "RETURN c.name AS category, sum(p.amount) AS totalAmount " +
                    "ORDER BY totalAmount DESC";
            default -> null;
        };

        if (cypher == null) return null;

        try (Session session = cardNeo4jDriver.session()) {
            List<Map<String, Object>> rows = session.run(cypher, Map.of("userUuid", userUuid.toString()))
                    .list(Record::asMap);
            return toJson(rows);
        }
    }

    private String queryMerchantToEtf(UUID userUuid) {
        String cypher =
                "MATCH (p:Payment)-[:EARNED]->(pt:Point)-[:CONVERTED_TO]->(e:ETF) " +
                "WHERE p.userUuid = $userUuid " +
                "RETURN p.merchant AS merchant, e.name AS etfName, sum(pt.amount) AS pointAmount " +
                "ORDER BY pointAmount DESC LIMIT 5";

        try (Session session = cardNeo4jDriver.session()) {
            List<Map<String, Object>> rows = session.run(cypher, Map.of("userUuid", userUuid.toString()))
                    .list(Record::asMap);
            return toJson(rows);
        }
    }

    private String querySecuritiesNeo4j(QueryIntent intent, UUID userUuid) {
        String cypher = switch (intent) {
            case ETF_HOLDINGS ->
                    "MATCH (a:Account)-[:HOLDS]->(e:ETF) " +
                    "WHERE a.userUuid = $userUuid " +
                    "RETURN e.name AS etfName, e.quantity AS quantity, e.currentValue AS currentValue " +
                    "ORDER BY currentValue DESC";
            case ETF_AMOUNT ->
                    "MATCH (a:Account)-[:HOLDS]->(e:ETF) " +
                    "WHERE a.userUuid = $userUuid " +
                    "RETURN sum(e.buyAmount) AS totalBuyAmount, " +
                    "sum(e.currentValue - e.buyAmount) AS totalProfitLoss";
            default -> null;
        };

        if (cypher == null) return null;

        try (Session session = securitiesNeo4jDriver.session()) {
            List<Map<String, Object>> rows = session.run(cypher, Map.of("userUuid", userUuid.toString()))
                    .list(Record::asMap);
            return toJson(rows);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
