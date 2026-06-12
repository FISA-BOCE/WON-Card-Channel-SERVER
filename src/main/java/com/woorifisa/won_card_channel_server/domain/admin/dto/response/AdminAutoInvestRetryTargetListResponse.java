package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import java.util.List;

public record AdminAutoInvestRetryTargetListResponse(
        AdminAutoInvestRetryTargetSummaryResponse summary,
        List<AdminAutoInvestRetryTargetItemResponse> items,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}
