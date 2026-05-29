package com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AutoInvestSubscriptionChangeResponse(
        UUID autoInvestSubscriptionUuid,
        PreviousEtf previousEtf,
        NewEtf newEtf
) {

    public record PreviousEtf(
            String etfName,
            String ticker,
            LocalDateTime effectiveTo
    ) {
    }

    public record NewEtf(
            Long etfId,
            String etfName,
            String ticker,
            LocalDateTime effectiveFrom
    ) {
    }
}
