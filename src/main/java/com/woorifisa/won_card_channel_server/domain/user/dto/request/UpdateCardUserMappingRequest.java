package com.woorifisa.won_card_channel_server.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateCardUserMappingRequest(
        @NotNull UUID cardUserUuid
) {
}
