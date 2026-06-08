package com.woorifisa.won_card_channel_server.domain.aidb.service;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.InvestmentPath;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.MyPointInvestmentPathResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jEtfResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jQueryResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jQueryType;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SameEtfAveragePointResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SweepExecutionResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.exception.AiDbErrorCode;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository.MonthlySweepRequestRow;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository.Neo4jQueryMappingException;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository.SameEtfAveragePointRow;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "features.neo4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Neo4jQueryServiceImpl implements Neo4jQueryService {

    private static final int DEFAULT_INVESTMENT_PATH_LIMIT = 20;

    private final CardAiNeo4jQueryRepository neo4jQueryRepository;

    @Override
    public Neo4jQueryResponse query(AiDbQueryRequest request) {
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

    private SameEtfAveragePointResponse querySameEtfAveragePoint(AiDbQueryRequest request) {
        try {
            SameEtfAveragePointRow row = neo4jQueryRepository.findMonthlySameEtfAveragePointAmount(
                            request.userUuid(),
                            request.params().baseMonth()
                    )
                    .orElseThrow(() -> new BusinessException(AiDbErrorCode.QUERY_RESULT_NOT_FOUND));

            Neo4jEtfResponse selectedEtf = new Neo4jEtfResponse(
                    row.selectedEtfId(),
                    row.selectedEtfTicker(),
                    row.selectedEtfName()
            );

            return new SameEtfAveragePointResponse(
                    Neo4jQueryType.SAME_ETF_AVERAGE_POINT,
                    request.userUuid(),
                    selectedEtf,
                    row.sameEtfUserCount(),
                    row.averagePointAmount(),
                    row.totalPointAmount(),
                    request.params().baseMonth()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Neo4jQueryMappingException e) {
            throw new BusinessException(AiDbErrorCode.GRAPH_QUERY_FAILED, e);
        } catch (Neo4jException e) {
            throw new BusinessException(AiDbErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    private MyPointInvestmentPathResponse queryMyPointInvestmentPath(AiDbQueryRequest request) {
        try {
            List<MonthlySweepRequestRow> rows = neo4jQueryRepository.findMonthlySweepRequests(
                    request.userUuid(),
                    request.params().baseMonth(),
                    DEFAULT_INVESTMENT_PATH_LIMIT
            );

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
        } catch (Neo4jQueryMappingException e) {
            throw new BusinessException(AiDbErrorCode.GRAPH_QUERY_FAILED, e);
        } catch (Neo4jException e) {
            throw new BusinessException(AiDbErrorCode.GRAPH_QUERY_FAILED, e);
        }
    }

    private InvestmentPath toInvestmentPath(MonthlySweepRequestRow row) {
        Neo4jEtfResponse targetEtf = new Neo4jEtfResponse(
                row.etfId(),
                row.ticker(),
                row.etfName()
        );

        SweepExecutionResponse execution = null;
        if (row.sweepId() != null) {
            execution = new SweepExecutionResponse(
                    row.sweepId(),
                    row.sweepStatus(),
                    row.receivedAt(),
                    row.startedAt(),
                    row.executionCompletedAt(),
                    row.failReason()
            );
        }

        return new InvestmentPath(
                row.sweepRequestId(),
                row.pointAmount(),
                row.krwAmount(),
                row.requestStatus(),
                row.requestedAt(),
                row.requestCompletedAt(),
                targetEtf,
                execution
        );
    }
}
