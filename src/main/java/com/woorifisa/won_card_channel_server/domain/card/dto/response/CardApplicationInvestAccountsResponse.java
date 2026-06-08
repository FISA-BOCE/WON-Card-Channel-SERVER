package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.util.List;
import java.util.UUID;

public record CardApplicationInvestAccountsResponse(
        List<Account> accounts
) {

    public record Account(
            UUID invstAccountUuid,
            String accountNoDisplay,
            boolean isLinked
    ) {
    }
}
