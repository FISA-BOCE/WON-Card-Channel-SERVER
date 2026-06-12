package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

public record AdminSweepRequestSummaryResponse(
        long totalCount,
        long createdCount,
        long processingCount,
        long completedCount,
        long failedCount
) {
}
