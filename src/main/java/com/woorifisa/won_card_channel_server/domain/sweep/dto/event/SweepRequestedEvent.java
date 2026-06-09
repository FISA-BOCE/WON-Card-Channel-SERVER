package com.woorifisa.won_card_channel_server.domain.sweep.dto.event;

import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SweepRequestedEvent(
        String eventId, String eventType, String correlationId, String idempotencyKey, Long sweepRequestId,
        UUID userUuid, UUID cardUserUuid,
        Long performanceId, Long pointLedgerId, String baseMonth, Long pointAmount, Long krwAmount,
        Long etfId, LocalDateTime requestedAt
) {

    public static SweepRequestedEvent from(Sweep sweep, String eventId) {
        return new SweepRequestedEvent(
                eventId,
                SweepEventType.SWEEP_REQUESTED.name(),
                sweep.getCorrelationId(),
                sweep.getIdempotencyKey(),
                sweep.getSweepRequestId(),
                sweep.getUserUuid(),
                sweep.getCardUserUuid(),
                sweep.getPerformanceId(),
                sweep.getPointLedgerId(),
                sweep.getBaseMonth(),
                sweep.getPointAmount(),
                sweep.getKrwAmount(),
                sweep.getEtfId(),
                sweep.getRequestedAt()
        );
    }
}