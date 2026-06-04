package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

import java.time.LocalDateTime;

public record SweepExecutionResponse(
        Long sweepId,
        String sweepStatus,
        LocalDateTime receivedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String failReason
) {
}
