package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

public record AdminOutboxEventSummaryResponse(
        long totalCount,
        long publishedCount,
        long failedCount,
        long retryingCount,
        long pendingCount
) {
}
