package com.woorifisa.won_card_channel_server.domain.reward.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardGetCurrentResponse;
import com.woorifisa.won_card_channel_server.domain.reward.exception.code.RewardErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.exception.code.PerformanceErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.external.CardCoreRewardApi;
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
public class RewardGetCurrentMonthService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final BigDecimal MINIMUM_REWARD_SATISFIED_AMOUNT = new BigDecimal("1000");
    private static final String REWARD_STATUS_SATISFIED = "기준 충족";
    private static final String REWARD_STATUS_NOT_SATISFIED = "기준 미달";

    private final CardChnPerformanceSummaryRepository performanceSummaryRepository;
    private final CardCoreRewardApi cardCoreRewardApi;

    public RewardGetCurrentResponse getCurrentMonthReward(AuthenticatedUser authenticatedUser) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        String baseMonth = YearMonth.now(SEOUL_ZONE_ID).toString();

        return performanceSummaryRepository.findByUserUuidAndBaseMonth(userUuid, baseMonth)
                .map(this::toResponse)
                .orElseGet(() -> getPreviousPerformanceFromCardCore(userUuid, baseMonth));
    }

    private RewardGetCurrentResponse getPreviousPerformanceFromCardCore(UUID userUuid, String baseMonth) {
        try {
            ApiResponse<RewardGetCurrentResponse> coreResponse = cardCoreRewardApi.getCurrentMonthReward(userUuid);
            if (coreResponse == null || coreResponse.data() == null) {
                throw new BusinessException(RewardErrorCode.REWARD_LEDGER_NOT_FOUND);
            }

            RewardGetCurrentResponse coreData = coreResponse.data();
            if (!baseMonth.equals(coreData.baseMonth())) {
                throw new BusinessException(RewardErrorCode.INVALID_REWARD_RESPONSE);
            }

            return normalizeRewardStatus(coreData);
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            throw new BusinessException(RewardErrorCode.REWARD_LEDGER_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(RewardErrorCode.REWARD_INFORMATION_UNAVAILABLE, e);
        }
    }

    private RewardGetCurrentResponse toResponse(CardChnPerformanceSummary performanceSummary) {
        String rewardStatus = getRewardStatus(performanceSummary.getPreviousMonthSpendAmount());

        return new RewardGetCurrentResponse(
                performanceSummary.getBaseMonth(),
                rewardStatus,
                toLong(performanceSummary.getPreviousMonthSpendAmount()),
                toLong(performanceSummary.getRewardPointAmount()),
                performanceSummary.getRewardRate(),
                performanceSummary.getPerformanceStatus()
        );
    }

    private RewardGetCurrentResponse normalizeRewardStatus(RewardGetCurrentResponse response) {
        String rewardStatus = getRewardStatus(toBigDecimal(response.previousMonthSpendAmount()));

        return new RewardGetCurrentResponse(
                response.baseMonth(),
                rewardStatus,
                response.previousMonthSpendAmount(),
                response.rewardPointAmount(),
                response.rewardRate(),
                response.performanceStatus()
        );
    }

    private String getRewardStatus(BigDecimal previousMonthSpendAmount) {
        if (previousMonthSpendAmount == null || previousMonthSpendAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(PerformanceErrorCode.INVALID_PERFORMANCE_AMOUNT);
        }

        if (previousMonthSpendAmount.compareTo(MINIMUM_REWARD_SATISFIED_AMOUNT) < 0) {
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

    private BigDecimal toBigDecimal(Long amount) {
        if (amount == null) {
            return null;
        }

        return BigDecimal.valueOf(amount);
    }
}
