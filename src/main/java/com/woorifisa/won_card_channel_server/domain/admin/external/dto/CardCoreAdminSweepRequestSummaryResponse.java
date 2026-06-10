package com.woorifisa.won_card_channel_server.domain.admin.external.dto;

public record CardCoreAdminSweepRequestSummaryResponse(
        long totalCount,
        long createdCount,
        long processingCount,
        long completedCount,
        long failedCount
) {
}
