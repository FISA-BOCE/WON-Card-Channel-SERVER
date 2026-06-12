package com.woorifisa.won_card_channel_server.domain.admin.external.dto;

import java.util.List;

public record CardCoreAdminSweepRequestListResponse(
        CardCoreAdminSweepRequestSummaryResponse summary,
        List<CardCoreAdminSweepRequestItemResponse> items,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}
