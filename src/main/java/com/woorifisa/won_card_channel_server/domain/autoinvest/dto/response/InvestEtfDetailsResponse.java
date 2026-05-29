package com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvestEtfDetailsResponse(
        @NotNull Long etfId,
        @NotBlank String etfName,
        @NotBlank String ticker,
        @NotNull Boolean isTradeAvailable,
        @NotNull Boolean isFractionalAvailable
) {
}
