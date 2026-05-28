package com.woorifisa.won_card_channel_server.domain.spend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SpendCurrentAmountResponse(
        boolean hasCurrentSpendAmount,
        String baseMonth,
        Long currentSpendAmount,
        BigDecimal currentRewardRate,
        String nextPerformanceStatus,
        Long amountRemainingUntilNextPerformance,
        BigDecimal nextRewardRate,
        List<RewardRange> rewardRanges,
        ExpectedReward expectedReward
) {

    public record RewardRange(
            Long min,
            Long max,
            BigDecimal rate
    ) {
    }

    public record ExpectedReward(
            Long targetSpendAmount,
            BigDecimal rewardRate,
            Long expectedRewardAmount
    ) {
    }
}
