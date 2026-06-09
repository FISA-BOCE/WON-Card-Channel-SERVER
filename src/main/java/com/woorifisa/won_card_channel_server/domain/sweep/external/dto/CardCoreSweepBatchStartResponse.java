package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

public record CardCoreSweepBatchStartResponse(
        Long batchExecutionId,
        String baseMonth,
        String status,
        long requestedCount
) {
}
