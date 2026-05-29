package com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record InvestAccountDetailsResponse(
        @JsonProperty("investAccountUuid") UUID investAccountUuid,
        UUID userUuid,
        String accountStatus
) {
}
