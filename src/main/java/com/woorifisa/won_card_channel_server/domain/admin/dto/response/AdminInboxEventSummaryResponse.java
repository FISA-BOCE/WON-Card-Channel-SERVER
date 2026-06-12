package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

public record AdminInboxEventSummaryResponse(
        long totalCount,
        long processedCount,
        long failedCount,
        long processingCount,
        long receivedCount
) {
}
