package com.woorifisa.won_card_channel_server.domain.admin.external.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CardCoreAdminSweepRequestItemResponse(
        Long sweepRequestId,
        Long pointLedgerId,
        UUID cardUserUuid,
        String baseMonth,
        Long pointAmount,
        String sweepStatus,
        String failureCode,
        String failureMessage,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
