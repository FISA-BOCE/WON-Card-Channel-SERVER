package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AutoSweepCreateCommand(
        @NotNull
        UUID userUuid,

        @NotNull
        UUID cardUserUuid,

        @NotNull
        Long pointLedgerId,

        @NotNull
        Long etfId
) {
}
