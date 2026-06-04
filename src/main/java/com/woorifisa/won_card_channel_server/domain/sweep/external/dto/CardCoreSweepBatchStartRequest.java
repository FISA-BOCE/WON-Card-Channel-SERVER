package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

public record CardCoreSweepBatchStartRequest(
        String baseMonth,
        Integer chunkSize
) {
}
