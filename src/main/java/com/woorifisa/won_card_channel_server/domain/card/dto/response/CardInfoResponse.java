package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.util.List;

public record CardInfoResponse(
        List<CardInfo> cards
) {

    public record CardInfo(
            String cardName,
            String cardNoDisplay
    ) {
    }
}
