package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

public record AdminAutoInvestRetryTargetSummaryResponse(
        long retryableCount,
        long retryingCount,
        long retrySucceededCount,
        long retryFailedCount
) {
}
