package com.woorifisa.won_card_channel_server.domain.sweep.dto.response;

public record AutoSweepBatchResponse(
        String baseMonth,
        int candidateCount,
        int requestedCount,
        int skippedCount,
        int failedCount
) {
}
