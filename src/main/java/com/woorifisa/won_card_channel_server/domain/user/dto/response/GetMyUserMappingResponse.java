package com.woorifisa.won_card_channel_server.domain.user.dto.response;

import java.util.UUID;

public record GetMyUserMappingResponse(
        Long mappingId,
        UUID userUuid,
        UUID cardUserUuid,
        UUID investUserUuid,
        String cardLinkStatus,
        String investLinkStatus
) {
}
