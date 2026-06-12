package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

public record AdminDashboardKpiResponse(
        long monthlySweepRequestCount,
        long monthlySweepCompletedCount,
        long monthlySweepFailedCount,
        long monthlyOutboxFailedCount,
        long monthlyInboxFailedCount
) {
}
