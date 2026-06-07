package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CardCoreSweepReservedItemResponse(
        Long sweepRequestId,
        String eventType,
        String eventId,
        String correlationId,
        String idempotencyKey,
        UUID cardUserUuid,
        Long performanceId,
        Long pointLedgerId,
        String baseMonth,
        Long pointAmount,
        Long krwAmount,
        LocalDateTime requestedAt
) {
}
