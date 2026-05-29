package com.woorifisa.won_card_channel_server.domain.performance.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.dto.response.PreviousPerformanceResponse;
import com.woorifisa.won_card_channel_server.domain.performance.exception.code.PerformanceErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.external.CardCorePerformanceApi;
import com.woorifisa.won_card_channel_server.domain.performance.model.CardChnPerformanceSummary;
import com.woorifisa.won_card_channel_server.domain.performance.repository.CardChnPerformanceSummaryRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformanceSummaryService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String PREVIOUS_MONTH_QUERY_PARAM = "previousMonth";
    private static final String REWARD_STATUS_SATISFIED = "기준 충족";
    private static final String REWARD_STATUS_NOT_SATISFIED = "기준 미달";

    private final CardChnPerformanceSummaryRepository performanceSummaryRepository;
    private final CardCorePerformanceApi cardCorePerformanceApi;

    public PreviousPerformanceResponse getPreviousPerformance(
            AuthenticatedUser authenticatedUser,
            Map<String, String> queryParams
    ) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        YearMonth previousMonth = resolvePreviousMonth(queryParams);
        YearMonth baseMonth = previousMonth.plusMonths(1);

        return performanceSummaryRepository.findByUserUuidAndBaseMonth(userUuid, baseMonth.toString())
                .map(performanceSummary -> toResponse(performanceSummary, previousMonth.toString()))
                .orElseGet(() -> getPreviousPerformanceFromCardCore(userUuid, queryParams, previousMonth));
    }

    private PreviousPerformanceResponse getPreviousPerformanceFromCardCore(
            UUID userUuid,
            Map<String, String> queryParams,
            YearMonth previousMonth
    ) {
        try {
            ApiResponse<PreviousPerformanceResponse> coreResponse = cardCorePerformanceApi.getMonthlyPerformance(
                    userUuid,
                    toCoreQueryParams(queryParams, previousMonth)
            );
            if (coreResponse == null || coreResponse.data() == null) {
                throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
            }

            return coreResponse.data();
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND, e);
        }
    }

    private Map<String, String> toCoreQueryParams(Map<String, String> queryParams, YearMonth previousMonth) {
        Map<String, String> coreQueryParams = new LinkedHashMap<>();
        if (queryParams != null) {
            coreQueryParams.putAll(queryParams);
        }
        coreQueryParams.put(PREVIOUS_MONTH_QUERY_PARAM, previousMonth.toString());
        return coreQueryParams;
    }

    private PreviousPerformanceResponse toResponse(
            CardChnPerformanceSummary performanceSummary,
            String previousMonth
    ) {
        String rewardStatus = getRewardStatus(performanceSummary);
        Long previousMonthSpendAmount = toLong(performanceSummary.getPreviousMonthSpendAmount());
        Long rewardPointAmount = toLong(performanceSummary.getRewardPointAmount());

        return new PreviousPerformanceResponse(
                performanceSummary.getBaseMonth(),
                previousMonth,
                rewardStatus,
                previousMonthSpendAmount,
                new PreviousPerformanceResponse.Detail(
                        previousMonthSpendAmount,
                        rewardPointAmount
                )
        );
    }

    private String getRewardStatus(CardChnPerformanceSummary performanceSummary) {
        BigDecimal previousMonthSpendAmount = performanceSummary.getPreviousMonthSpendAmount();
        if (previousMonthSpendAmount == null || previousMonthSpendAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(PerformanceErrorCode.INVALID_PERFORMANCE_AMOUNT);
        }

        if (BigDecimal.ZERO.compareTo(previousMonthSpendAmount) == 0) {
            return REWARD_STATUS_NOT_SATISFIED;
        }

        return REWARD_STATUS_SATISFIED;
    }

    private YearMonth resolvePreviousMonth(Map<String, String> queryParams) {
        String previousMonth = queryParams == null ? null : queryParams.get(PREVIOUS_MONTH_QUERY_PARAM);
        if (previousMonth == null || previousMonth.isBlank()) {
            return YearMonth.now(SEOUL_ZONE_ID).minusMonths(1);
        }

        try {
            return YearMonth.parse(previousMonth, YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(PerformanceErrorCode.INVALID_QUERY_MONTH, e);
        }
    }

    private UUID extractUserUuid(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userUuid() == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        return authenticatedUser.userUuid();
    }

    private Long toLong(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }

        return amount.setScale(0, RoundingMode.DOWN).longValue();
    }
}
