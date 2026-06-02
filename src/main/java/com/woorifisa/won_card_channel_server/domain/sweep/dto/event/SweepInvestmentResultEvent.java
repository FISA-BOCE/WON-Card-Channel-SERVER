package com.woorifisa.won_card_channel_server.domain.sweep.dto.event;

import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;

import java.util.UUID;

public record SweepInvestmentResultEvent(
        String eventId,
        SweepEventType eventType,
        String correlationId,
        String idempotencyKey,
        Long sweepRequestId,
        Long sweepExecutionId,
        Long pointLedgerId,
        UUID userUuid,
        UUID cardUserUuid,
        String baseMonth,
        Long pointAmount,
        Long krwAmount,
        Long etfId,
        String failureCode,
        String failureMessage
) {
    public boolean completed() {
        return eventType == SweepEventType.SWEEP_INVESTMENT_COMPLETED;
    }

    public boolean failed() {
        return eventType == SweepEventType.SWEEP_INVESTMENT_FAILED;
    }
}
