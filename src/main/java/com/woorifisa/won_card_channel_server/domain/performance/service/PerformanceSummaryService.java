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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformanceSummaryService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final String REWARD_STATUS_SATISFIED = "기준 충족";
    private static final String REWARD_STATUS_NOT_SATISFIED = "기준 미달";

    private final CardChnPerformanceSummaryRepository performanceSummaryRepository;
    private final CardCorePerformanceApi cardCorePerformanceApi;

    public PreviousPerformanceResponse getPreviousPerformance(AuthenticatedUser authenticatedUser) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        String baseMonth = YearMonth.now(SEOUL_ZONE_ID).toString();

        return performanceSummaryRepository.findByUserUuidAndBaseMonth(userUuid, baseMonth)
                .map(this::toResponse)
                .orElseGet(() -> getPreviousPerformanceFromCardCore(userUuid));
    }

    private PreviousPerformanceResponse getPreviousPerformanceFromCardCore(UUID userUuid) {
        try {
            ApiResponse<PreviousPerformanceResponse> coreResponse = cardCorePerformanceApi.getMonthlyPerformance(userUuid);
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

    private PreviousPerformanceResponse toResponse(CardChnPerformanceSummary performanceSummary) {
        String rewardStatus = getRewardStatus(performanceSummary);

        return new PreviousPerformanceResponse(
                performanceSummary.getBaseMonth(),
                rewardStatus,
                toLong(performanceSummary.getPreviousMonthSpendAmount()),
                toLong(performanceSummary.getRewardPointAmount()),
                performanceSummary.getRewardRate(),
                performanceSummary.getPerformanceStatus()
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
