package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public record CardCoreSweepResultRequest(
        @NotNull
        Long sweepRequestId,

        @NotNull
        Long sweepExecutionId,

        @NotBlank
        String correlationId,

        @NotBlank
        String idempotencyKey,

        @NotBlank
        String resultStatus
) {
}
