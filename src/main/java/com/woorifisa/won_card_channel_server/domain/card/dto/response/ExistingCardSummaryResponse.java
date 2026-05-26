package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExistingCardSummaryResponse(
        boolean hasCard,
        String cardUuid,
        String cardName,
        String cardNoDisplay,
        String cardStatus,
        UsageSummary usageSummary
) {

    public ExistingCardSummaryResponse(
            String cardUuid,
            String cardName,
            String cardNoDisplay,
            String cardStatus,
            UsageSummary usageSummary
    ) {
        this(true, cardUuid, cardName, cardNoDisplay, cardStatus, usageSummary);
    }

    public record UsageSummary(
            BigDecimal currentMonthUsageAmount,
            List<RewardRange> rewardRanges,
            BigDecimal currentRewardRate,
            Long currentRangeMin,
            Long currentRangeMax
    ) {
    }

    public record RewardRange(
            Long min,
            Long max,
            BigDecimal rate
    ) {
    }
}