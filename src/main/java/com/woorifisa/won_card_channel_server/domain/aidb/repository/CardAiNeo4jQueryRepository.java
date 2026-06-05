package com.woorifisa.won_card_channel_server.domain.aidb.repository;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SweepExecutionStatus;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SweepRequestStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

@Repository
public class CardAiNeo4jQueryRepository {

    private static final String FIND_MONTHLY_SAME_ETF_AVERAGE_POINT_AMOUNT =
            loadQuery("neo4j/queries/find-monthly-same-etf-average-point-amount.cypher");
    private static final String FIND_MONTHLY_SWEEP_REQUESTS =
            loadQuery("neo4j/queries/find-monthly-sweep-requests.cypher");

    private final Driver cardNeo4jDriver;

    public CardAiNeo4jQueryRepository(@Qualifier("cardNeo4jDriver") Driver cardNeo4jDriver) {
        this.cardNeo4jDriver = cardNeo4jDriver;
    }

    private static String loadQuery(String path) {
        try (var inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load Neo4j query: " + path, e);
        }
    }

    public Optional<SameEtfAveragePointRow> findMonthlySameEtfAveragePointAmount(UUID userUuid, String baseMonth) {
        Map<String, Object> parameters = Map.of(
                "userUuid", userUuid.toString(),
                "baseMonth", baseMonth,
                "completedRequestStatus", SweepRequestStatus.COMPLETED.name()
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
                toLong(required(row, "selectedEtfId")),
                toStringValue(required(row, "selectedEtfTicker")),
                toStringValue(required(row, "selectedEtfName")),
                toLong(required(row, "sameEtfUserCount")),
                toBigDecimal(required(row, "averagePointAmount")),
                toBigDecimal(required(row, "totalPointAmount"))
        );
    }

    private MonthlySweepRequestRow toMonthlySweepRequestRow(Map<String, Object> row) {
        Object sweepId = row.get("sweepId");

        return new MonthlySweepRequestRow(
                toLong(required(row, "sweepRequestId")),
                toBigDecimal(required(row, "pointAmount")),
                toBigDecimal(required(row, "krwAmount")),
                toSweepRequestStatus(required(row, "requestStatus")),
                toLocalDateTime(required(row, "requestedAt")),
                toLocalDateTime(row.get("requestCompletedAt")),
                toLong(required(row, "etfId")),
                toStringValue(required(row, "ticker")),
                toStringValue(required(row, "etfName")),
                toLong(sweepId),
                toSweepExecutionStatus(sweepId == null ? row.get("sweepStatus") : required(row, "sweepStatus")),
                toLocalDateTime(row.get("receivedAt")),
                toLocalDateTime(row.get("startedAt")),
                toLocalDateTime(row.get("executionCompletedAt")),
                toStringValue(row.get("failReason"))
        );
    }

    private Object required(Map<String, Object> row, String fieldName) {
        Object value = row.get(fieldName);
        if (value == null) {
            throw new Neo4jQueryMappingException("Missing required Neo4j field: " + fieldName);
        }
        return value;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new Neo4jQueryMappingException("Failed to convert Neo4j value to Long: " + value, e);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
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
        } catch (NumberFormatException e) {
            throw new Neo4jQueryMappingException("Failed to convert Neo4j value to BigDecimal: " + value, e);
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
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
        } catch (DateTimeParseException e) {
            throw new Neo4jQueryMappingException("Failed to convert Neo4j value to LocalDateTime: " + value, e);
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private SweepRequestStatus toSweepRequestStatus(Object value) {
        try {
            return value == null ? null : SweepRequestStatus.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new Neo4jQueryMappingException("Failed to convert Neo4j value to SweepRequestStatus: " + value, e);
        }
    }

    private SweepExecutionStatus toSweepExecutionStatus(Object value) {
        try {
            return value == null ? null : SweepExecutionStatus.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new Neo4jQueryMappingException("Failed to convert Neo4j value to SweepExecutionStatus: " + value, e);
        }
    }

    public static class Neo4jQueryMappingException extends RuntimeException {

        public Neo4jQueryMappingException(String message) {
            super(message);
        }

        public Neo4jQueryMappingException(String message, Throwable cause) {
            super(message, cause);
        }
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
            SweepRequestStatus requestStatus,
            LocalDateTime requestedAt,
            LocalDateTime requestCompletedAt,
            Long etfId,
            String ticker,
            String etfName,
            Long sweepId,
            SweepExecutionStatus sweepStatus,
            LocalDateTime receivedAt,
            LocalDateTime startedAt,
            LocalDateTime executionCompletedAt,
            String failReason
    ) {
    }
}
