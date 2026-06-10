package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import java.util.List;

public record AdminOutboxEventListResponse(
        AdminOutboxEventSummaryResponse summary,
        List<AdminOutboxEventItemResponse> items,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}
