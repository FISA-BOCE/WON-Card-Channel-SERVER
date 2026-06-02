package com.woorifisa.won_card_channel_server.domain.ai.graph.service;

import com.woorifisa.won_card_channel_server.domain.ai.graph.dto.response.GraphRelationResponse;
import com.woorifisa.won_card_channel_server.domain.ai.graph.dto.response.GraphRelationResponse.GraphLink;
import com.woorifisa.won_card_channel_server.domain.ai.graph.dto.response.GraphRelationResponse.GraphNode;
import com.woorifisa.won_card_channel_server.domain.ai.graph.exception.GraphErrorCode;
import com.woorifisa.won_card_channel_server.domain.ai.graph.model.GraphQueryType;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class GraphRelationService {

    private static final String CYPHER_MERCHANT_TO_ETF =
            "MATCH (p:Payment)-[:EARNED]->(pt:Point)-[:CONVERTED_TO]->(e:ETF) " +
            "WHERE p.userUuid = $userUuid " +
            "AND p.merchant = $merchant " +
            "AND p.paymentDate >= date() - duration({months: $periodMonths}) " +
            "RETURN p.merchant AS merchant, e.name AS etfName, sum(pt.amount) AS pointAmount " +
            "ORDER BY pointAmount DESC LIMIT 10";

    private static final String CYPHER_POINT_BY_MERCHANT =
            "MATCH (p:Payment)-[:EARNED]->(pt:Point) " +
            "WHERE p.userUuid = $userUuid " +
            "AND p.paymentDate >= date() - duration({months: $periodMonths}) " +
            "RETURN p.merchant AS merchant, sum(pt.amount) AS totalPoints " +
            "ORDER BY totalPoints DESC LIMIT 10";

    private static final String CYPHER_SPEND_BY_CATEGORY =
            "MATCH (p:Payment)-[:BELONGS_TO]->(c:Category) " +
            "WHERE p.userUuid = $userUuid " +
            "AND p.paymentDate >= date() - duration({months: $periodMonths}) " +
            "RETURN c.name AS category, sum(p.amount) AS totalAmount " +
            "ORDER BY totalAmount DESC";

    private static final String CYPHER_SPEND_BY_CATEGORY_WITH_FILTER =
            "MATCH (p:Payment)-[:BELONGS_TO]->(c:Category) " +
            "WHERE p.userUuid = $userUuid " +
            "AND p.paymentDate >= date() - duration({months: $periodMonths}) " +
            "AND c.name = $category " +
            "RETURN c.name AS category, sum(p.amount) AS totalAmount " +
            "ORDER BY totalAmount DESC";

    private final Driver cardNeo4jDriver;

    public GraphRelationService(@Qualifier("cardNeo4jDriver") Driver cardNeo4jDriver) {
        this.cardNeo4jDriver = cardNeo4jDriver;
    }

    public GraphRelationResponse getGraphRelations(
            UUID userUuid, String queryTypeStr, String period, String merchant, String category) {

        GraphQueryType queryType = parseQueryType(queryTypeStr);
        validateParams(queryType, merchant);

        int periodMonths = parsePeriodMonths(period);
        List<Map<String, Object>> rows = queryNeo4j(queryType, userUuid, periodMonths, merchant, category);

        return buildResponse(queryType, rows, period, merchant, category);
    }

    private GraphQueryType parseQueryType(String queryTypeStr) {
        try {
            return GraphQueryType.valueOf(queryTypeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(GraphErrorCode.UNSUPPORTED_QUERY_TYPE);
        }
    }

    private void validateParams(GraphQueryType queryType, String merchant) {
        if (queryType == GraphQueryType.MERCHANT_TO_ETF && (merchant == null || merchant.isBlank())) {
            throw new BusinessException(GraphErrorCode.MERCHANT_REQUIRED);
        }
    }

    private List<Map<String, Object>> queryNeo4j(
            GraphQueryType queryType, UUID userUuid, int periodMonths, String merchant, String category) {

        String cypher = buildCypher(queryType, category);
        Map<String, Object> params = buildParams(queryType, userUuid, periodMonths, merchant, category);

        try (Session session = cardNeo4jDriver.session()) {
            return session.run(cypher, params).list(Record::asMap);
        } catch (Exception e) {
            log.error("Neo4j query failed: queryType={}, userUuid={}", queryType, userUuid, e);
            throw new BusinessException(GraphErrorCode.GRAPH_DB_ERROR, e);
        }
    }

    private String buildCypher(GraphQueryType queryType, String category) {
        return switch (queryType) {
            case MERCHANT_TO_ETF -> CYPHER_MERCHANT_TO_ETF;
            case POINT_BY_MERCHANT -> CYPHER_POINT_BY_MERCHANT;
            case SPEND_BY_CATEGORY -> (category != null && !category.isBlank())
                    ? CYPHER_SPEND_BY_CATEGORY_WITH_FILTER
                    : CYPHER_SPEND_BY_CATEGORY;
        };
    }

    private Map<String, Object> buildParams(
            GraphQueryType queryType, UUID userUuid, int periodMonths, String merchant, String category) {

        Map<String, Object> params = new HashMap<>();
        params.put("userUuid", userUuid.toString());
        params.put("periodMonths", periodMonths);
        if (queryType == GraphQueryType.MERCHANT_TO_ETF) {
            params.put("merchant", merchant);
        }
        if (queryType == GraphQueryType.SPEND_BY_CATEGORY && category != null && !category.isBlank()) {
            params.put("category", category);
        }
        return params;
    }

    private GraphRelationResponse buildResponse(
            GraphQueryType queryType, List<Map<String, Object>> rows,
            String period, String merchant, String category) {

        return switch (queryType) {
            case MERCHANT_TO_ETF -> buildMerchantToEtfResponse(rows, period, merchant);
            case POINT_BY_MERCHANT -> buildPointByMerchantResponse(rows, period);
            case SPEND_BY_CATEGORY -> buildSpendByCategoryResponse(rows, period, category);
        };
    }

    private GraphRelationResponse buildMerchantToEtfResponse(
            List<Map<String, Object>> rows, String period, String merchant) {

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphLink> links = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String idx = String.format("%02d", i + 1);
            String spendId = "SPEND_" + idx;
            String pointId = "POINT_" + idx;
            String assetId = "ASSET_" + idx;

            long pointAmount = toLong(row.getOrDefault("pointAmount", 0L));
            String etfName = (String) row.getOrDefault("etfName", "");

            nodes.add(new GraphNode(spendId, "소비", merchant + " 결제"));
            nodes.add(new GraphNode(pointId, "리워드", pointAmount + " 포인트 적립"));
            nodes.add(new GraphNode(assetId, "자산", etfName + " ETF"));
            links.add(new GraphLink(spendId, pointId, "EARNED"));
            links.add(new GraphLink(pointId, assetId, "CONVERTED_TO"));
        }

        String topEtf = rows.isEmpty() ? "" : (String) rows.get(0).getOrDefault("etfName", "");
        String summary = String.format(
                "최근 %s간 %s 결제를 통해 적립된 포인트가 %s ETF 투자의 주요 원천이 되었습니다.",
                parsePeriodLabel(period), merchant, topEtf);

        return new GraphRelationResponse(GraphQueryType.MERCHANT_TO_ETF.name(), nodes, links, summary);
    }

    private GraphRelationResponse buildPointByMerchantResponse(
            List<Map<String, Object>> rows, String period) {

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphLink> links = new ArrayList<>();

        long totalPoints = rows.stream().mapToLong(r -> toLong(r.getOrDefault("totalPoints", 0L))).sum();
        nodes.add(new GraphNode("POINT_01", "리워드", totalPoints + " 포인트 총 적립"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String spendId = "SPEND_" + String.format("%02d", i + 1);
            String merchantName = (String) row.getOrDefault("merchant", "");
            long points = toLong(row.getOrDefault("totalPoints", 0L));

            nodes.add(new GraphNode(spendId, "소비", merchantName + " " + points + "P 적립"));
            links.add(new GraphLink(spendId, "POINT_01", "EARNED"));
        }

        String topMerchant = rows.isEmpty() ? "" : (String) rows.get(0).getOrDefault("merchant", "");
        String summary = String.format(
                "최근 %s간 총 %d 포인트가 적립되었으며, %s에서 가장 많이 적립되었습니다.",
                parsePeriodLabel(period), totalPoints, topMerchant);

        return new GraphRelationResponse(GraphQueryType.POINT_BY_MERCHANT.name(), nodes, links, summary);
    }

    private GraphRelationResponse buildSpendByCategoryResponse(
            List<Map<String, Object>> rows, String period, String category) {

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphLink> links = new ArrayList<>();

        nodes.add(new GraphNode("SPEND_00", "소비", "전체 지출"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String categoryId = "CATEGORY_" + String.format("%02d", i + 1);
            String categoryName = (String) row.getOrDefault("category", "");
            long amount = toLong(row.getOrDefault("totalAmount", 0L));

            nodes.add(new GraphNode(categoryId, "카테고리", categoryName + " " + amount + "원"));
            links.add(new GraphLink("SPEND_00", categoryId, "BELONGS_TO"));
        }

        String topCategory = rows.isEmpty() ? "" : (String) rows.get(0).getOrDefault("category", "");
        long topAmount = rows.isEmpty() ? 0L : toLong(rows.get(0).getOrDefault("totalAmount", 0L));
        String summary = String.format(
                "최근 %s간 %s에서 지출이 가장 많았습니다. 총 %d원입니다.",
                parsePeriodLabel(period), topCategory, topAmount);

        return new GraphRelationResponse(GraphQueryType.SPEND_BY_CATEGORY.name(), nodes, links, summary);
    }

    private int parsePeriodMonths(String period) {
        if (period == null) return 1;
        return switch (period) {
            case "3MONTH" -> 3;
            case "6MONTH" -> 6;
            case "1YEAR" -> 12;
            default -> 1;
        };
    }

    private String parsePeriodLabel(String period) {
        if (period == null) return "1개월";
        return switch (period) {
            case "3MONTH" -> "3개월";
            case "6MONTH" -> "6개월";
            case "1YEAR" -> "1년";
            default -> "1개월";
        };
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}
