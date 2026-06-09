package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CardCoreApplicationResponse(
        UUID cardUuid,
        String cardNoDisplay,
        LocalDateTime issuedAt,
        String cardStatus
) {
}
