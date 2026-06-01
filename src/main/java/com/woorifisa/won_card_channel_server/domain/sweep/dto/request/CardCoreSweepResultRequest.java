package com.woorifisa.won_card_channel_server.domain.sweep.dto.request;

public record CardCoreSweepResultRequest(
        Long sweepRequestId,
        Long sweepExecutionId,
        String correlationId,
        String idempotencyKey,
        String resultStatus
) {
}
