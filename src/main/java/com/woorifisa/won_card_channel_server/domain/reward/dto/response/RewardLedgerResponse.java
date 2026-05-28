package com.woorifisa.won_card_channel_server.domain.reward.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record RewardLedgerResponse(
        int baseYear,
        Long totalAccumulatedAmount,
        List<RewardLedgerItem> ledgers
) {
    public record RewardLedgerItem(
            Long pointLedgerId,
            String baseMonth,
            Long pointAmount,
            String type,
            LocalDateTime occurredAt
    ) {
    }
}
