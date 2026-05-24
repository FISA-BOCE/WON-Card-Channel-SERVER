package com.woorifisa.won_card_channel_server.domain.reward.dto.response;

import java.time.LocalDateTime;

public record RewardLedgerDetailResponse(
        Long pointLedgerId,
        String baseMonth,
        String type,
        Long pointAmount,
        LocalDateTime occurredAt,
        RewardDetail detail
) {
    public record RewardDetail(
            Long previousMonthSpendAmount,
            Long targetSpendAmount,
            Long shortfallAmount
    ) {
    }
}
