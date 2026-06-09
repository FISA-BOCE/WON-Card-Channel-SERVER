package com.woorifisa.won_card_channel_server.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GetMyUserMappingResponse(
        UUID userUuid,
        CardMapping card,
        InvestMapping invest
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardMapping(
            UUID cardUserUuid,
            Boolean isConnected
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InvestMapping(
            UUID investUserUuid,
            Boolean isConnected
    ) {
    }
}
