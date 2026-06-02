package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardCoreSweepResultResponse(
        @NotNull
        Long pointLedgerId,

        @NotBlank
        String sweepStatus
) {
}
