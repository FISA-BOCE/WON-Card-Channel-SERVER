package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

import java.util.List;

public record CardCoreSweepReservationResponse(
        Long batchExecutionId,
        String baseMonth,
        String status,
        int reservedCount,
        Long lastProcessedPointLedgerId,
        List<CardCoreSweepReservedItemResponse> reservedItems
) {
}
