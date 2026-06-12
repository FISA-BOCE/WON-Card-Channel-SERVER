package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import java.util.List;

public record AdminDashboardSummaryResponse(
        String baseMonth,
        AdminDashboardKpiResponse kpis,
        AdminSweepRequestSummaryResponse sweepSummary,
        AdminOutboxEventSummaryResponse outboxSummary,
        AdminInboxEventSummaryResponse inboxSummary,
        List<AdminSweepRequestItemResponse> recentFailedSweepRequests,
        List<AdminOutboxEventItemResponse> recentFailedOutboxEvents,
        List<AdminInboxEventItemResponse> recentFailedInboxEvents
) {
}
