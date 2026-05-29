package com.woorifisa.won_card_channel_server.domain.user.dto.request;

import java.util.UUID;

public record UpdateCardUserMappingRequest(
        UUID cardUserUuid
) {
}
