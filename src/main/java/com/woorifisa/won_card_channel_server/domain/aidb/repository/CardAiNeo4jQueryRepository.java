package com.woorifisa.won_card_channel_server.domain.aidb.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class CardAiNeo4jQueryRepository {

    private static final String FIND_MONTHLY_SAME_ETF_AVERAGE_POINT_AMOUNT = """
            MATCH (me:User {userUuid: $userUuid})-[:SELECTED]->(etf:ETF)
            MATCH (other:User)-[:SELECTED]->(etf)
            WHERE other.userUuid <> me.userUuid
            MATCH (other)-[:REQUESTED_SWEEP]->(sr:SweepRequest)-[:TARGETS]->(etf)
            WHERE sr.requestStatus = 'SUCCEEDED'
              AND sr.baseMonth = $baseMonth
            WITH
                me,
                etf,
                other,
                sum(sr.pointAmount) AS userTotalPointAmount
            RETURN
                me.displayName AS userName,
                etf.etfId AS selectedEtfId,
                etf.ticker AS selectedEtfTicker,
                etf.etfName AS selectedEtfName,
                $baseMonth AS baseMonth,
                count(other) AS sameEtfUserCount,
                avg(userTotalPointAmount) AS averagePointAmount,
                sum(userTotalPointAmount) AS totalPointAmount
            """;

    private static final String FIND_MONTHLY_SWEEP_REQUESTS = """
            MATCH (u:User {userUuid: $userUuid})
                  -[:REQUESTED_SWEEP]->(sr:SweepRequest)
                  -[:TARGETS]->(etf:ETF)
            WHERE sr.baseMonth = $baseMonth
            OPTIONAL MATCH (sr)-[:EXECUTED_AS]->(se:SweepExecution)
            RETURN
                u.displayName AS userName,
                sr.sweepRequestId AS sweepRequestId,
                sr.baseMonth AS baseMonth,
                sr.pointAmount AS pointAmount,
                sr.krwAmount AS krwAmount,
                sr.requestStatus AS requestStatus,
                sr.requestedAt AS requestedAt,
                sr.completedAt AS requestCompletedAt,
                etf.etfId AS etfId,
                etf.ticker AS ticker,
                etf.etfName AS etfName,
                se.sweepId AS sweepId,
                se.sweepStatus AS sweepStatus,
                se.receivedAt AS receivedAt,
                se.startedAt AS startedAt,
                se.completedAt AS executionCompletedAt,
                se.failReason AS failReason
            ORDER BY sr.requestedAt DESC
            LIMIT $limit
            """;

    private final Driver cardNeo4jDriver;

    public CardAiNeo4jQueryRepository(@Qualifier("cardNeo4jDriver") Driver cardNeo4jDriver) {
        this.cardNeo4jDriver = cardNeo4jDriver;
    }

    public Optional<SameEtfAveragePointRow> findMonthlySameEtfAveragePointAmount(UUID userUuid, String baseMonth) {
        Map<String, Object> parameters = Map.of(
                "userUuid", userUuid.toString(),
                "baseMonth", baseMonth
        );

        try (var session = cardNeo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(FIND_MONTHLY_SAME_ETF_AVERAGE_POINT_AMOUNT, parameters)
                    .list(record -> toSameEtfAveragePointRow(record.asMap()))
                    .stream()
                    .findFirst());
        }
    }

    public List<MonthlySweepRequestRow> findMonthlySweepRequests(UUID userUuid, String baseMonth, int limit) {
        Map<String, Object> parameters = Map.of(
                "userUuid", userUuid.toString(),
                "baseMonth", baseMonth,
                "limit", limit
        );

        try (var session = cardNeo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(FIND_MONTHLY_SWEEP_REQUESTS, parameters)
                    .list(record -> toMonthlySweepRequestRow(record.asMap())));
        }
    }

    private SameEtfAveragePointRow toSameEtfAveragePointRow(Map<String, Object> row) {
        return new SameEtfAveragePointRow(
                toLong(row.get("selectedEtfId")),
                toStringValue(row.get("selectedEtfTicker")),
                toStringValue(row.get("selectedEtfName")),
                toLong(row.get("sameEtfUserCount")),
                toBigDecimal(row.get("averagePointAmount")),
                toBigDecimal(row.get("totalPointAmount"))
        );
    }

    private MonthlySweepRequestRow toMonthlySweepRequestRow(Map<String, Object> row) {
        return new MonthlySweepRequestRow(
                toLong(row.get("sweepRequestId")),
                toBigDecimal(row.get("pointAmount")),
                toBigDecimal(row.get("krwAmount")),
                toStringValue(row.get("requestStatus")),
                toLocalDateTime(row.get("requestedAt")),
                toLocalDateTime(row.get("requestCompletedAt")),
                toLong(row.get("etfId")),
                toStringValue(row.get("ticker")),
                toStringValue(row.get("etfName")),
                toLong(row.get("sweepId")),
                toStringValue(row.get("sweepStatus")),
                toLocalDateTime(row.get("receivedAt")),
                toLocalDateTime(row.get("startedAt")),
                toLocalDateTime(row.get("executionCompletedAt")),
                toStringValue(row.get("failReason"))
        );
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDateTime();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString());
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    public record SameEtfAveragePointRow(
            Long selectedEtfId,
            String selectedEtfTicker,
            String selectedEtfName,
            Long sameEtfUserCount,
            BigDecimal averagePointAmount,
            BigDecimal totalPointAmount
    ) {
    }

    public record MonthlySweepRequestRow(
            Long sweepRequestId,
            BigDecimal pointAmount,
            BigDecimal krwAmount,
            String requestStatus,
            LocalDateTime requestedAt,
            LocalDateTime requestCompletedAt,
            Long etfId,
            String ticker,
            String etfName,
            Long sweepId,
            String sweepStatus,
            LocalDateTime receivedAt,
            LocalDateTime startedAt,
            LocalDateTime executionCompletedAt,
            String failReason
    ) {
    }
}
