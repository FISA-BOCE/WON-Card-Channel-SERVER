package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

public record CardCoreSweepResultRequest(
        Long sweepRequestId,
        Long sweepExecutionId,
        String correlationId,
        String idempotencyKey,
        String resultStatus
) {
}
