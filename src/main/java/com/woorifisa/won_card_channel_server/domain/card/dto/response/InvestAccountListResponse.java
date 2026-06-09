package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.util.List;
import java.util.UUID;

public record InvestAccountListResponse(
        List<Account> accounts
) {

    public record Account(
            UUID investAccountUuid,
            String accountNoDisplay,
            String accountStatus
    ) {
    }
}
