package com.woorifisa.won_card_channel_server.domain.spend.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.model.CardChnPerformanceSummary;
import com.woorifisa.won_card_channel_server.domain.performance.repository.CardChnPerformanceSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.spend.dto.response.SpendCurrentAmountResponse;
import com.woorifisa.won_card_channel_server.domain.spend.exception.code.SpendErrorCode;
import com.woorifisa.won_card_channel_server.domain.spend.external.CardCoreSpendApi;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpendSummaryService {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Long FIRST_PERFORMANCE_MIN_AMOUNT = 0L;
    private static final Long SECOND_PERFORMANCE_MIN_AMOUNT = 500_000L;
    private static final Long THIRD_PERFORMANCE_MIN_AMOUNT = 1_500_000L;
    private static final Long MIN_REWARD_POINT_LIMIT_AMOUNT = 0L;
    private static final Long MAX_REWARD_POINT_LIMIT_AMOUNT = 40_000L;

    private static final String FIRST_PERFORMANCE_STATUS = "1";
    private static final String SECOND_PERFORMANCE_STATUS = "2";
    private static final String THIRD_PERFORMANCE_STATUS = "3";

    private static final BigDecimal FIRST_REWARD_RATE = new BigDecimal("0.7");
    private static final BigDecimal SECOND_REWARD_RATE = new BigDecimal("1.0");
    private static final BigDecimal THIRD_REWARD_RATE = new BigDecimal("1.2");
    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");

    private final CardChnPerformanceSummaryRepository performanceSummaryRepository;
    private final CardCoreSpendApi cardCoreSpendApi;

    public SpendCurrentAmountResponse getSpendSummary(AuthenticatedUser authenticatedUser) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        String baseMonth = YearMonth.now(SEOUL_ZONE_ID).toString();

        return performanceSummaryRepository.findByUserUuidAndBaseMonth(userUuid, baseMonth)
                .map(this::toResponse)
                .orElseGet(() -> getSpendSummaryFromCardCore(userUuid));
    }

    private SpendCurrentAmountResponse getSpendSummaryFromCardCore(UUID userUuid) {
        try {
            ApiResponse<SpendCurrentAmountResponse> coreResponse = cardCoreSpendApi.getSpendSummary(userUuid);
            if (coreResponse == null || coreResponse.data() == null) {
                throw new BusinessException(SpendErrorCode.INVALID_SPEND_RESPONSE);
            }

            SpendCurrentAmountResponse coreData = coreResponse.data();
            validateCoreSpendData(coreData);
            return toResponse(coreData);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(SpendErrorCode.CURRENT_SPEND_AMOUNT_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(SpendErrorCode.SPEND_INFORMATION_UNAVAILABLE, e);
        }
    }

    private SpendCurrentAmountResponse toResponse(CardChnPerformanceSummary performanceSummary) {
        Long currentSpendAmount = toLong(performanceSummary.getCurrentMonthSpendAmount());
        String performanceStatus = calculatePerformanceStatus(currentSpendAmount);
        String nextPerformanceStatus = getNextPerformanceStatus(performanceStatus);
        BigDecimal currentRewardRate = getRewardRate(performanceStatus);
        BigDecimal nextRewardRate = getRewardRate(nextPerformanceStatus);
        Long amountRemainingUntilNextPerformance = calculateAmountRemainingUntilNextPerformance(
                currentSpendAmount,
                performanceStatus
        );
        Long expectedRewardAmount = calculateExpectedRewardAmount(currentSpendAmount, currentRewardRate);

        return new SpendCurrentAmountResponse(
                true,
                performanceSummary.getBaseMonth(),
                currentSpendAmount,
                currentRewardRate,
                nextPerformanceStatus,
                amountRemainingUntilNextPerformance,
                nextRewardRate,
                rewardRanges(),
                new SpendCurrentAmountResponse.ExpectedReward(
                        currentSpendAmount,
                        currentRewardRate,
                        expectedRewardAmount
                )
        );
    }

    private SpendCurrentAmountResponse toResponse(SpendCurrentAmountResponse coreData) {
        return new SpendCurrentAmountResponse(
                coreData.hasCurrentSpendAmount(),
                coreData.baseMonth(),
                coreData.currentSpendAmount(),
                coreData.currentRewardRate(),
                coreData.nextPerformanceStatus(),
                coreData.amountRemainingUntilNextPerformance(),
                coreData.nextRewardRate(),
            rewardRanges(),
                coreData.expectedReward()
        );
    }

    private void validateCoreSpendData(SpendCurrentAmountResponse coreData) {
        if (!coreData.hasCurrentSpendAmount()
                || isBlank(coreData.baseMonth())
                || coreData.currentSpendAmount() == null
                || coreData.currentRewardRate() == null
                || isBlank(coreData.nextPerformanceStatus())
                || coreData.amountRemainingUntilNextPerformance() == null
                || coreData.nextRewardRate() == null
                || coreData.expectedReward() == null
                || coreData.expectedReward().targetSpendAmount() == null
                || coreData.expectedReward().rewardRate() == null
                || coreData.expectedReward().expectedRewardAmount() == null) {
            throw new BusinessException(SpendErrorCode.INVALID_SPEND_RESPONSE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<SpendCurrentAmountResponse.RewardRange> rewardRanges() {
        return List.of(
                new SpendCurrentAmountResponse.RewardRange(0L, 499_999L, FIRST_REWARD_RATE),
                new SpendCurrentAmountResponse.RewardRange(500_000L, 1_499_999L, SECOND_REWARD_RATE),
                new SpendCurrentAmountResponse.RewardRange(1_500_000L, null, THIRD_REWARD_RATE)
        );
    }

    private UUID extractUserUuid(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userUuid() == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        return authenticatedUser.userUuid();
    }

    private String calculatePerformanceStatus(Long currentSpendAmount) {
        if (currentSpendAmount < SECOND_PERFORMANCE_MIN_AMOUNT) {
            return FIRST_PERFORMANCE_STATUS;
        }
        if (currentSpendAmount < THIRD_PERFORMANCE_MIN_AMOUNT) {
            return SECOND_PERFORMANCE_STATUS;
        }
        return THIRD_PERFORMANCE_STATUS;
    }

    private String getNextPerformanceStatus(String performanceStatus) {
        if (FIRST_PERFORMANCE_STATUS.equals(performanceStatus)) {
            return SECOND_PERFORMANCE_STATUS;
        }
        return THIRD_PERFORMANCE_STATUS;
    }

    private BigDecimal getRewardRate(String performanceStatus) {
        return switch (performanceStatus) {
            case FIRST_PERFORMANCE_STATUS -> FIRST_REWARD_RATE;
            case SECOND_PERFORMANCE_STATUS -> SECOND_REWARD_RATE;
            case THIRD_PERFORMANCE_STATUS -> THIRD_REWARD_RATE;
            default -> BigDecimal.ZERO;
        };
    }

    private Long calculateAmountRemainingUntilNextPerformance(Long currentSpendAmount, String performanceStatus) {
        Long nextPerformanceMinAmount = switch (performanceStatus) {
            case FIRST_PERFORMANCE_STATUS -> SECOND_PERFORMANCE_MIN_AMOUNT;
            case SECOND_PERFORMANCE_STATUS, THIRD_PERFORMANCE_STATUS -> THIRD_PERFORMANCE_MIN_AMOUNT;
            default -> FIRST_PERFORMANCE_MIN_AMOUNT;
        };

        return Math.max(nextPerformanceMinAmount - currentSpendAmount, 0L);
    }

    private Long calculateExpectedRewardAmount(Long currentSpendAmount, BigDecimal rewardRate) {
        Long expectedRewardAmount = BigDecimal.valueOf(currentSpendAmount)
                .multiply(rewardRate)
                .divide(PERCENT_DIVISOR, 0, RoundingMode.DOWN)
                .longValue();

        return Math.max(
                MIN_REWARD_POINT_LIMIT_AMOUNT,
                Math.min(expectedRewardAmount, MAX_REWARD_POINT_LIMIT_AMOUNT)
        );
    }

    private Long toLong(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }

        return amount.setScale(0, RoundingMode.DOWN).longValue();
    }
}
