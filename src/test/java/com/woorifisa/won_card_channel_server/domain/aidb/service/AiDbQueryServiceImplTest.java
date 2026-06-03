package com.woorifisa.won_card_channel_server.domain.aidb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.AiDbQueryResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.AiDbQueryType;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.CardMonthlyTotalSpendResult;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.PointCurrentBalanceResult;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.PointMonthlyEarnedResult;
import com.woorifisa.won_card_channel_server.domain.aidb.exception.AiDbErrorCode;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardChnAiSpendSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.aidb.repository.CardChnAiSpendSummaryRepository.SpendAmountSummary;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

@ExtendWith(MockitoExtension.class)
class AiDbQueryServiceImplTest {

    private static final UUID USER_UUID = UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5");
    private static final String BASE_MONTH = "2025-06";

    @Mock
    private CardChnAiSpendSummaryRepository spendSummaryRepository;

    @Mock
    private SpendAmountSummary spendAmountSummary;

    private AiDbQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiDbQueryServiceImpl(spendSummaryRepository);
    }

    @Test
    @DisplayName("CARD_MONTHLY_TOTAL_SPEND returns monthly spend summary")
    void queryMonthlyTotalSpend() {
        given(spendAmountSummary.getTotalSpendAmount()).willReturn(decimal("1000000"));
        given(spendAmountSummary.getFoodAmount()).willReturn(decimal("200000"));
        given(spendAmountSummary.getShoppingAmount()).willReturn(decimal("500000"));
        given(spendAmountSummary.getTransportAmount()).willReturn(decimal("100000"));
        given(spendAmountSummary.getSubscriptionAmount()).willReturn(decimal("100000"));
        given(spendAmountSummary.getEtcAmount()).willReturn(decimal("100000"));
        given(spendSummaryRepository.findSpendAmountSummaryByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(spendAmountSummary));

        AiDbQueryResponse<?> response = service.query(request("CARD_MONTHLY_TOTAL_SPEND", BASE_MONTH));

        assertThat(response.queryType()).isEqualTo(AiDbQueryType.CARD_MONTHLY_TOTAL_SPEND);
        assertThat(response.result()).isInstanceOf(CardMonthlyTotalSpendResult.class);

        CardMonthlyTotalSpendResult result = (CardMonthlyTotalSpendResult) response.result();
        assertThat(result.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(result.totalSpendAmount()).isEqualByComparingTo("1000000");
        assertThat(result.foodAmount()).isEqualByComparingTo("200000");
        assertThat(result.shoppingAmount()).isEqualByComparingTo("500000");
        assertThat(result.transportAmount()).isEqualByComparingTo("100000");
        assertThat(result.subscriptionAmount()).isEqualByComparingTo("100000");
        assertThat(result.etcAmount()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("POINT_CURRENT_BALANCE returns accumulated current point")
    void queryCurrentPointBalance() {
        given(spendSummaryRepository.existsByUserUuidAndBaseMonthLessThanEqual(USER_UUID, BASE_MONTH))
                .willReturn(true);
        given(spendSummaryRepository.calculateCurrentPointAmount(USER_UUID, BASE_MONTH))
                .willReturn(decimal("2800"));

        AiDbQueryResponse<?> response = service.query(request("POINT_CURRENT_BALANCE", BASE_MONTH));

        assertThat(response.queryType()).isEqualTo(AiDbQueryType.POINT_CURRENT_BALANCE);
        assertThat(response.result()).isInstanceOf(PointCurrentBalanceResult.class);

        PointCurrentBalanceResult result = (PointCurrentBalanceResult) response.result();
        assertThat(result.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(result.currentPoint()).isEqualByComparingTo("2800");
    }

    @Test
    @DisplayName("POINT_MONTHLY_EARNED returns current month earned point")
    void queryMonthlyEarnedPoint() {
        given(spendSummaryRepository.findCurrentMonthEarnedAmountByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(decimal("36000")));

        AiDbQueryResponse<?> response = service.query(request("POINT_MONTHLY_EARNED", BASE_MONTH));

        assertThat(response.queryType()).isEqualTo(AiDbQueryType.POINT_MONTHLY_EARNED);
        assertThat(response.result()).isInstanceOf(PointMonthlyEarnedResult.class);

        PointMonthlyEarnedResult result = (PointMonthlyEarnedResult) response.result();
        assertThat(result.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(result.currentMonthEarnedAmount()).isEqualByComparingTo("36000");
    }

    @Test
    @DisplayName("unsupported queryType throws CHAT_400_003")
    void unsupportedQueryType() {
        assertThatThrownBy(() -> service.query(request("UNKNOWN_QUERY", BASE_MONTH)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.UNSUPPORTED_QUERY_TYPE));
    }

    @Test
    @DisplayName("null queryType throws CHAT_400_003")
    void nullQueryType() {
        assertThatThrownBy(() -> service.query(request(null, BASE_MONTH)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.UNSUPPORTED_QUERY_TYPE));
    }

    @Test
    @DisplayName("invalid baseMonth throws CHAT_400_004")
    void invalidBaseMonth() {
        assertThatThrownBy(() -> service.query(request("CARD_MONTHLY_TOTAL_SPEND", "2025-99")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.INVALID_QUERY_PARAM));
    }

    @Test
    @DisplayName("no query result throws CHAT_404_001")
    void resultNotFound() {
        given(spendSummaryRepository.findCurrentMonthEarnedAmountByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.query(request("POINT_MONTHLY_EARNED", BASE_MONTH)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.QUERY_RESULT_NOT_FOUND));
    }

    @Test
    @DisplayName("DataAccessException is mapped to CHAT_500_001")
    void mysqlQueryFailed() {
        given(spendSummaryRepository.findCurrentMonthEarnedAmountByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willThrow(new DataRetrievalFailureException("db down"));

        assertThatThrownBy(() -> service.query(request("POINT_MONTHLY_EARNED", BASE_MONTH)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiDbErrorCode.MYSQL_QUERY_FAILED));
    }

    @Test
    @DisplayName("queryType is trimmed before mapping")
    void queryTypeTrimmed() {
        given(spendSummaryRepository.findCurrentMonthEarnedAmountByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(decimal("36000")));

        service.query(request(" POINT_MONTHLY_EARNED ", BASE_MONTH));

        verify(spendSummaryRepository).findCurrentMonthEarnedAmountByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH);
    }

    private AiDbQueryRequest request(String queryType, String baseMonth) {
        return new AiDbQueryRequest(USER_UUID, queryType, new AiDbQueryRequest.Params(baseMonth));
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
