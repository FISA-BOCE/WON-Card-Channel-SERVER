package com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response;

import java.util.UUID;

public record InvestAccountDetailsResponse(
        UUID invstAccountUuid,
        UUID userUuid,
        String accountStatus
) {
}
