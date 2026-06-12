package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import java.util.List;

public record AdminSweepRequestListResponse(
        AdminSweepRequestSummaryResponse summary,
        List<AdminSweepRequestItemResponse> items,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}
