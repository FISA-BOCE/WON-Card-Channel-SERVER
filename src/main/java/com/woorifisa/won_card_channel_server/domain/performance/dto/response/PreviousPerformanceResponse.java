package com.woorifisa.won_card_channel_server.domain.performance.dto.response;

import java.math.BigDecimal;

public record PreviousPerformanceResponse(
        String baseMonth,
        String rewardStatus,
        Long previousMonthSpendAmount,
        Long rewardPointAmount,
        BigDecimal rewardRate,
        String performanceStatus
) {
}
