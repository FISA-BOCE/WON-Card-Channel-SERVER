package com.woorifisa.won_card_channel_server.domain.aidb.service;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.Neo4jQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.InvestmentPath;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.MyPointInvestmentPathResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jEtfResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jQueryType;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SameEtfAveragePointResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SweepExecutionResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.exception.AiDbErrorCode;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Neo4jQueryServiceImpl implements Neo4jQueryService {

    private static final int DEFAULT_INVESTMENT_PATH_LIMIT = 20;

    private final CardAiNeo4jQueryRepository neo4jQueryRepository;

    @Override
    public Object query(Neo4jQueryRequest request) {
        Neo4jQueryType queryType = parseQueryType(request.queryType());
        validateBaseMonth(request.params().baseMonth());

        return switch (queryType) {
            case SAME_ETF_AVERAGE_POINT -> querySameEtfAveragePoint(request);
            case MY_POINT_INVESTMENT_PATH -> queryMyPointInvestmentPath(request);
        };
    }

    private Neo4jQueryType parseQueryType(String queryType) {
        try {
            return Neo4jQueryType.valueOf(queryType.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(AiDbErrorCode.UNSUPPORTED_QUERY_TYPE, e);
        }
    }

    private void validateBaseMonth(String baseMonth) {
        try {
            YearMonth.parse(baseMonth);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(AiDbErrorCode.INVALID_QUERY_PARAM, e);
        }
    }

    private SameEtfAveragePointResponse querySameEtfAveragePoint(Neo4jQueryRequest request) {
        try {
            Map<String, Object> row = neo4jQueryRepository.findMonthlySameEtfAveragePointAmount(
                            request.userUuid(),
                            request.params().baseMonth()
                    )
                    .orElseThrow(() -> new BusinessException(AiDbErrorCode.QUERY_RESULT_NOT_FOUND));

            Neo4jEtfResponse selectedEtf = new Neo4jEtfResponse(
                    toLong(row.get("selectedEtfId")),
                    toStringValue(row.get("selectedEtfTicker")),
                    toStringValue(row.get("selectedEtfName"))
            );

            return new SameEtfAveragePointResponse(
                    Neo4jQueryType.SAME_ETF_AVERAGE_POINT,
                    request.userUuid(),
                    selectedEtf,
                    toLong(row.get("sameEtfUserCount")),
                    toBigDecimal(row.get("averagePointAmount")),
                    toBigDecimal(row.get("totalPointAmount")),
                    request.params().baseMonth()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Neo4jException e) {
            throw new BusinessException(AiDbErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    private MyPointInvestmentPathResponse queryMyPointInvestmentPath(Neo4jQueryRequest request) {
        try {
            List<Map<String, Object>> rows = neo4jQueryRepository.findMonthlySweepRequests(
                    request.userUuid(),
                    request.params().baseMonth(),
                    DEFAULT_INVESTMENT_PATH_LIMIT
            );

            if (rows.isEmpty()) {
                throw new BusinessException(AiDbErrorCode.QUERY_RESULT_NOT_FOUND);
            }

            return new MyPointInvestmentPathResponse(
                    Neo4jQueryType.MY_POINT_INVESTMENT_PATH,
                    request.userUuid(),
                    request.params().baseMonth(),
                    rows.stream()
                            .map(this::toInvestmentPath)
                            .toList()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Neo4jException e) {
            throw new BusinessException(AiDbErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    private InvestmentPath toInvestmentPath(Map<String, Object> row) {
        Neo4jEtfResponse targetEtf = new Neo4jEtfResponse(
                toLong(row.get("etfId")),
                toStringValue(row.get("ticker")),
                toStringValue(row.get("etfName"))
        );

        SweepExecutionResponse execution = null;
        if (row.get("sweepId") != null) {
            execution = new SweepExecutionResponse(
                    toLong(row.get("sweepId")),
                    toStringValue(row.get("sweepStatus")),
                    toLocalDateTime(row.get("receivedAt")),
                    toLocalDateTime(row.get("startedAt")),
                    toLocalDateTime(row.get("executionCompletedAt")),
                    toStringValue(row.get("failReason"))
            );
        }

        return new InvestmentPath(
                toLong(row.get("sweepRequestId")),
                toBigDecimal(row.get("pointAmount")),
                toBigDecimal(row.get("krwAmount")),
                toStringValue(row.get("requestStatus")),
                toLocalDateTime(row.get("requestedAt")),
                toLocalDateTime(row.get("requestCompletedAt")),
                targetEtf,
                execution
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
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
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
}
