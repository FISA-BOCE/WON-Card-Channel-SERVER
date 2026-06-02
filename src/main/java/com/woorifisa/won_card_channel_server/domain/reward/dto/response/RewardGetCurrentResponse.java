package com.woorifisa.won_card_channel_server.domain.reward.dto.response;

import java.math.BigDecimal;

public record RewardGetCurrentResponse(
        String baseMonth,
        String rewardStatus,
        Long previousMonthSpendAmount,
        Long rewardPointAmount,
        BigDecimal rewardRate,
        String performanceStatus
) {
}
