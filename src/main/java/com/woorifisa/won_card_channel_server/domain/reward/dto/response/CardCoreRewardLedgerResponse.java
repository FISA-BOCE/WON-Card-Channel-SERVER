package com.woorifisa.won_card_channel_server.domain.reward.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CardCoreRewardLedgerResponse(
        int baseYear,
        Long totalAccumulatedAmount,
        List<CardCoreRewardLedgerItem> ledgers
) {
    public record CardCoreRewardLedgerItem(Long pointLedgerId, String baseMonth, Long pointAmount,
                                           String type, String sweepStatus, String sweepFailureCode,
                                           String sweepFailureMessage, LocalDateTime occurredAt) {
    }
}
