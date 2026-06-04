package com.woorifisa.won_card_channel_server.domain.aidb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.InvestmentPath;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.MyPointInvestmentPathResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jQueryResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jQueryType;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SameEtfAveragePointResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SweepExecutionStatus;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SweepExecutionResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.SweepRequestStatus;
import com.woorifisa.won_card_channel_server.domain.aidb.exception.AiDbErrorCode;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository.MonthlySweepRequestRow;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardAiNeo4jQueryRepository.SameEtfAveragePointRow;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

@ExtendWith(MockitoExtension.class)
class Neo4jQueryServiceImplTest {

    private static final UUID USER_UUID = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private static final String BASE_MONTH = "2025-06";
    private static final int DEFAULT_LIMIT = 20;

    @Mock
    private CardAiNeo4jQueryRepository neo4jQueryRepository;

    private Neo4jQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new Neo4jQueryServiceImpl(neo4jQueryRepository);
    }

    @Test
    @DisplayName("SAME_ETF_AVERAGE_POINT returns same ETF average point response")
    void querySameEtfAveragePoint() {
        given(neo4jQueryRepository.findMonthlySameEtfAveragePointAmount(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(new SameEtfAveragePointRow(
                        1L,
                        "SPY",
                        "SPDR S&P 500 ETF Trust",
                        3L,
                        decimal("15000.50"),
                        decimal("45001.50")
                )));

        Neo4jQueryResponse response = service.query(request("SAME_ETF_AVERAGE_POINT", BASE_MONTH));

        assertThat(response).isInstanceOf(SameEtfAveragePointResponse.class);
        SameEtfAveragePointResponse result = (SameEtfAveragePointResponse) response;
        assertThat(result.queryType()).isEqualTo(Neo4jQueryType.SAME_ETF_AVERAGE_POINT);
        assertThat(result.userUuid()).isEqualTo(USER_UUID);
        assertThat(result.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(result.selectedEtf().etfId()).isEqualTo(1L);
        assertThat(result.selectedEtf().ticker()).isEqualTo("SPY");
        assertThat(result.selectedEtf().etfName()).isEqualTo("SPDR S&P 500 ETF Trust");
        assertThat(result.sameEtfUserCount()).isEqualTo(3L);
        assertThat(result.averagePointAmount()).isEqualByComparingTo("15000.50");
        assertThat(result.totalPointAmount()).isEqualByComparingTo("45001.50");
    }

    @Test
    @DisplayName("SAME_ETF_AVERAGE_POINT returns zero summary when no other user selected same ETF")
    void querySameEtfAveragePointWithoutOtherUsers() {
        given(neo4jQueryRepository.findMonthlySameEtfAveragePointAmount(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(new SameEtfAveragePointRow(
                        1L,
                        "SPY",
                        "SPDR S&P 500 ETF Trust",
                        0L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )));

        Neo4jQueryResponse response = service.query(request("SAME_ETF_AVERAGE_POINT", BASE_MONTH));

        assertThat(response).isInstanceOf(SameEtfAveragePointResponse.class);
        SameEtfAveragePointResponse result = (SameEtfAveragePointResponse) response;
        assertThat(result.sameEtfUserCount()).isZero();
        assertThat(result.averagePointAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPointAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("MY_POINT_INVESTMENT_PATH returns investment paths with optional execution")
    void queryMyPointInvestmentPath() {
        LocalDateTime requestedAt = LocalDateTime.of(2025, 6, 10, 9, 30);
        LocalDateTime requestCompletedAt = LocalDateTime.of(2025, 6, 10, 9, 35);
        LocalDateTime receivedAt = LocalDateTime.of(2025, 6, 10, 9, 40);
        LocalDateTime startedAt = LocalDateTime.of(2025, 6, 10, 9, 45);
        LocalDateTime executionCompletedAt = LocalDateTime.of(2025, 6, 10, 9, 50);

        MonthlySweepRequestRow executedRow = new MonthlySweepRequestRow(
                100L,
                decimal("12000"),
                decimal("12000"),
                SweepRequestStatus.COMPLETED,
                requestedAt,
                requestCompletedAt,
                1L,
                "SPY",
                "SPDR S&P 500 ETF Trust",
                200L,
                SweepExecutionStatus.COMPLETED,
                receivedAt,
                startedAt,
                executionCompletedAt,
                null
        );
        MonthlySweepRequestRow pendingRow = new MonthlySweepRequestRow(
                101L,
                decimal("8000"),
                decimal("8000"),
                SweepRequestStatus.READY,
                requestedAt.plusDays(1),
                null,
                2L,
                "QQQ",
                "Invesco QQQ Trust",
                null,
                null,
                null,
                null,
                null,
                null
        );
        given(neo4jQueryRepository.findMonthlySweepRequests(USER_UUID, BASE_MONTH, DEFAULT_LIMIT))
                .willReturn(List.of(executedRow, pendingRow));

        Neo4jQueryResponse response = service.query(request("MY_POINT_INVESTMENT_PATH", BASE_MONTH));

        assertThat(response).isInstanceOf(MyPointInvestmentPathResponse.class);
        MyPointInvestmentPathResponse result = (MyPointInvestmentPathResponse) response;
        assertThat(result.queryType()).isEqualTo(Neo4jQueryType.MY_POINT_INVESTMENT_PATH);
        assertThat(result.userUuid()).isEqualTo(USER_UUID);
        assertThat(result.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(result.investmentPaths()).hasSize(2);

        InvestmentPath executedPath = result.investmentPaths().get(0);
        assertThat(executedPath.sweepRequestId()).isEqualTo(100L);
        assertThat(executedPath.pointAmount()).isEqualByComparingTo("12000");
        assertThat(executedPath.krwAmount()).isEqualByComparingTo("12000");
        assertThat(executedPath.requestStatus()).isEqualTo(SweepRequestStatus.COMPLETED);
        assertThat(executedPath.requestedAt()).isEqualTo(requestedAt);
        assertThat(executedPath.completedAt()).isEqualTo(requestCompletedAt);
        assertThat(executedPath.targetEtf().ticker()).isEqualTo("SPY");

        SweepExecutionResponse execution = executedPath.execution();
        assertThat(execution).isNotNull();
        assertThat(execution.sweepId()).isEqualTo(200L);
        assertThat(execution.sweepStatus()).isEqualTo(SweepExecutionStatus.COMPLETED);
        assertThat(execution.receivedAt()).isEqualTo(receivedAt);
        assertThat(execution.startedAt()).isEqualTo(startedAt);
        assertThat(execution.completedAt()).isEqualTo(executionCompletedAt);
        assertThat(execution.failReason()).isNull();

        InvestmentPath pendingPath = result.investmentPaths().get(1);
        assertThat(pendingPath.sweepRequestId()).isEqualTo(101L);
        assertThat(pendingPath.targetEtf().ticker()).isEqualTo("QQQ");
        assertThat(pendingPath.execution()).isNull();

        verify(neo4jQueryRepository).findMonthlySweepRequests(USER_UUID, BASE_MONTH, DEFAULT_LIMIT);
    }

    @Test
    @DisplayName("unsupported queryType throws AIDB_400_003")
    void unsupportedQueryType() {
        assertThatThrownBy(() -> service.query(request("UNKNOWN_QUERY", BASE_MONTH)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.UNSUPPORTED_QUERY_TYPE));

        verifyNoInteractions(neo4jQueryRepository);
    }

    @Test
    @DisplayName("invalid baseMonth throws AIDB_400_004")
    void invalidBaseMonth() {
        assertThatThrownBy(() -> service.query(request("SAME_ETF_AVERAGE_POINT", "2025-99")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.INVALID_QUERY_PARAM));

        verifyNoInteractions(neo4jQueryRepository);
    }

    @Test
    @DisplayName("missing selected ETF result throws AIDB_404_001")
    void selectedEtfNotFound() {
        given(neo4jQueryRepository.findMonthlySameEtfAveragePointAmount(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.query(request("SAME_ETF_AVERAGE_POINT", BASE_MONTH)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.QUERY_RESULT_NOT_FOUND));
    }

    @Test
    @DisplayName("MY_POINT_INVESTMENT_PATH returns empty paths when no monthly investment exists")
    void queryMyPointInvestmentPathWithoutMonthlyInvestment() {
        given(neo4jQueryRepository.findMonthlySweepRequests(USER_UUID, BASE_MONTH, DEFAULT_LIMIT))
                .willReturn(List.of());

        Neo4jQueryResponse response = service.query(request("MY_POINT_INVESTMENT_PATH", BASE_MONTH));

        assertThat(response).isInstanceOf(MyPointInvestmentPathResponse.class);
        MyPointInvestmentPathResponse result = (MyPointInvestmentPathResponse) response;
        assertThat(result.queryType()).isEqualTo(Neo4jQueryType.MY_POINT_INVESTMENT_PATH);
        assertThat(result.userUuid()).isEqualTo(USER_UUID);
        assertThat(result.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(result.investmentPaths()).isEmpty();
    }

    @Test
    @DisplayName("Neo4jException is mapped to AIDB_500_002")
    void graphQueryFailed() {
        given(neo4jQueryRepository.findMonthlySameEtfAveragePointAmount(USER_UUID, BASE_MONTH))
                .willThrow(new ServiceUnavailableException("neo4j down"));

        assertThatThrownBy(() -> service.query(request("SAME_ETF_AVERAGE_POINT", BASE_MONTH)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.GRAPH_QUERY_FAILED);
                    assertThat(e.getCause()).isInstanceOf(ServiceUnavailableException.class);
                });
    }

    @Test
    @DisplayName("queryType is trimmed before mapping")
    void queryTypeTrimmed() {
        given(neo4jQueryRepository.findMonthlySweepRequests(USER_UUID, BASE_MONTH, DEFAULT_LIMIT))
                .willReturn(List.of(monthlySweepRequestRow()));

        service.query(request(" MY_POINT_INVESTMENT_PATH ", BASE_MONTH));

        verify(neo4jQueryRepository).findMonthlySweepRequests(USER_UUID, BASE_MONTH, DEFAULT_LIMIT);
    }

    private AiDbQueryRequest request(String queryType, String baseMonth) {
        return new AiDbQueryRequest(USER_UUID, queryType, new AiDbQueryRequest.Params(baseMonth));
    }

    private MonthlySweepRequestRow monthlySweepRequestRow() {
        return new MonthlySweepRequestRow(
                100L,
                decimal("12000"),
                decimal("12000"),
                SweepRequestStatus.COMPLETED,
                LocalDateTime.of(2025, 6, 10, 9, 30),
                LocalDateTime.of(2025, 6, 10, 9, 35),
                1L,
                "SPY",
                "SPDR S&P 500 ETF Trust",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}

