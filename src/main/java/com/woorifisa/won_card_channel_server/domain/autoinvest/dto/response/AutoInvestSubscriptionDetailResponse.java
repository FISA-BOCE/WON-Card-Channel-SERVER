package com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AutoInvestSubscriptionDetailResponse(
        UUID cardUuid,
        CurrentEtf currentEtf,
        PendingEtf pendingEtf,
        boolean isAutoInvestEnabled
) {

    public record CurrentEtf(
            Long etfId,
            String etfName,
            String ticker,
            LocalDateTime effectiveFrom
    ) {
    }

    public record PendingEtf(
            Long etfId,
            String etfName,
            String ticker,
            LocalDateTime effectiveFrom
    ) {
    }
}
