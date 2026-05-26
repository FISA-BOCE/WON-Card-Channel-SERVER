package com.woorifisa.won_card_channel_server.domain.sweep.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InternalSweepRequestCreateRequest(
        @NotNull
        UUID userUuid,

        @NotNull
        UUID cardUserUuid,

        @NotNull
        UUID investUserUuid,

        @NotNull
        UUID investAccountUuid,

        @NotNull
        Long pointLedgerId,

        @NotNull
        Long etfId,

        @NotBlank
        String ticker
) {
}
