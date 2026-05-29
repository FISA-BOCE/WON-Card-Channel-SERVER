package com.woorifisa.won_card_channel_server.domain.autoinvest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AutoInvestSubscriptionChangeRequest(
        @NotNull @Positive Long etfId
) {
}
