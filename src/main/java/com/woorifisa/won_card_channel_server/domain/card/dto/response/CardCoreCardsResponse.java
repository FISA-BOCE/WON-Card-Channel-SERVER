package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.math.BigDecimal;

public record CardCoreCardsResponse(
        boolean hasCard,
        String cardUuid,
        String cardNoDisplay,
        String cardStatus,
        UsageSummary usageSummary
) {

    public record UsageSummary(
            BigDecimal currentMonthUsageAmount
    ) {
    }
}