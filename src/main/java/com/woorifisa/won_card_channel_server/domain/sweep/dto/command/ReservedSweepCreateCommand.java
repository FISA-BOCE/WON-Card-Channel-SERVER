package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservedSweepCreateCommand(
        UUID userUuid,
        UUID cardUserUuid,
        Long performanceId,
        Long pointLedgerId,
        String baseMonth,
        Long pointAmount,
        Long krwAmount,
        Long etfId,
        String eventId,
        String correlationId,
        String idempotencyKey,
        LocalDateTime requestedAt
) {
}
