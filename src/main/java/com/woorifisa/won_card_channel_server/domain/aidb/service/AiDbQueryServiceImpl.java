package com.woorifisa.won_card_channel_server.domain.aidb.service;

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
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(prefix = "features.aidb", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiDbQueryServiceImpl implements AiDbQueryService {

    private final CardChnAiSpendSummaryRepository spendSummaryRepository;

    @Override
    public AiDbQueryResponse<?> query(AiDbQueryRequest request) {
        AiDbQueryType queryType = parseQueryType(request.queryType());
        validateBaseMonth(request.params().baseMonth());

        return switch (queryType) {
            case CARD_MONTHLY_TOTAL_SPEND -> queryMonthlyTotalSpend(request, queryType);
            case POINT_CURRENT_BALANCE -> queryCurrentPointBalance(request, queryType);
            case POINT_MONTHLY_EARNED -> queryMonthlyEarnedPoint(request, queryType);
        };
    }

    private AiDbQueryType parseQueryType(String queryType) {
        try {
            return AiDbQueryType.valueOf(queryType.trim());
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

    private AiDbQueryResponse<CardMonthlyTotalSpendResult> queryMonthlyTotalSpend(
            AiDbQueryRequest request,
            AiDbQueryType queryType
    ) {
        try {
            SpendAmountSummary summary = spendSummaryRepository.findSpendAmountSummaryByUserUuidAndBaseMonth(
                            request.userUuid(),
                            request.params().baseMonth()
                    )
                    .orElseThrow(() -> new BusinessException(AiDbErrorCode.QUERY_RESULT_NOT_FOUND));

            CardMonthlyTotalSpendResult result = new CardMonthlyTotalSpendResult(
                    request.params().baseMonth(),
                    summary.getTotalSpendAmount(),
                    summary.getFoodAmount(),
                    summary.getShoppingAmount(),
                    summary.getTransportAmount(),
                    summary.getSubscriptionAmount(),
                    summary.getEtcAmount()
            );

            return new AiDbQueryResponse<>(queryType, result);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new BusinessException(AiDbErrorCode.MYSQL_QUERY_FAILED, e);
        }
    }

    private AiDbQueryResponse<PointCurrentBalanceResult> queryCurrentPointBalance(
            AiDbQueryRequest request,
            AiDbQueryType queryType
    ) {
        try {
            boolean hasHistory = spendSummaryRepository.existsByUserUuidAndBaseMonthLessThanEqual(
                    request.userUuid(),
                    request.params().baseMonth()
            );

            if (!hasHistory) {
                throw new BusinessException(AiDbErrorCode.QUERY_RESULT_NOT_FOUND);
            }

            BigDecimal currentPoint = spendSummaryRepository.calculateCurrentPointAmount(
                    request.userUuid(),
                    request.params().baseMonth()
            );

            PointCurrentBalanceResult result = new PointCurrentBalanceResult(
                    request.params().baseMonth(),
                    currentPoint
            );

            return new AiDbQueryResponse<>(queryType, result);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new BusinessException(AiDbErrorCode.MYSQL_QUERY_FAILED, e);
        }
    }

    private AiDbQueryResponse<PointMonthlyEarnedResult> queryMonthlyEarnedPoint(
            AiDbQueryRequest request,
            AiDbQueryType queryType
    ) {
        try {
            BigDecimal earnedAmount = spendSummaryRepository.findCurrentMonthEarnedAmountByUserUuidAndBaseMonth(
                            request.userUuid(),
                            request.params().baseMonth()
                    )
                    .orElseThrow(() -> new BusinessException(AiDbErrorCode.QUERY_RESULT_NOT_FOUND));

            PointMonthlyEarnedResult result = new PointMonthlyEarnedResult(
                    request.params().baseMonth(),
                    earnedAmount
            );

            return new AiDbQueryResponse<>(queryType, result);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new BusinessException(AiDbErrorCode.MYSQL_QUERY_FAILED, e);
        }
    }
}
