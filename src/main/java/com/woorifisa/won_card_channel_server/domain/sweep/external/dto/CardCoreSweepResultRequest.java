package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
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
        String resultStatus,

        String failureCode,

        String failureMessage
) {
    public static CardCoreSweepResultRequest from(SweepInvestmentResultEvent event) {
        return new CardCoreSweepResultRequest(
                event.sweepRequestId(),
                event.sweepExecutionId(),
                event.correlationId(),
                event.idempotencyKey(),
                event.completed() ? "COMPLETED" : "FAILED",
                event.failureCode(),
                event.failureMessage()
        );
    }
}
