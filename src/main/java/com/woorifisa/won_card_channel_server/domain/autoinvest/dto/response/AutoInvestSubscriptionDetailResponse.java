package com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AutoInvestSubscriptionDetailResponse(
        UUID autoInvestSubscriptionUuid,
        CurrentEtf currentEtf,
        boolean isAutoInvestEnabled
) {

    public record CurrentEtf(
            Long etfId,
            String etfName,
            String ticker,
            LocalDateTime effectiveFrom
    ) {
    }
}
