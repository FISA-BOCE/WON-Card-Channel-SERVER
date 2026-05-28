package com.woorifisa.won_card_channel_server.domain.sweep.dto.event;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SweepRequestedEvent(
        String eventId, String eventType, String correlationId, String idempotencyKey, Long sweepRequestId,
        UUID userUuid, UUID cardUserUuid,
        Long performanceId, Long pointLedgerId, String baseMonth, Long pointAmount, Long krwAmount,
        Long etfId, LocalDateTime requestedAt
) {

    public static SweepRequestedEvent from(CardChnSweepRequest sweepRequest, String eventId) {
        return new SweepRequestedEvent(
                eventId,
                SweepEventType.SWEEP_REQUESTED.name(),
                sweepRequest.getCorrelationId(),
                sweepRequest.getIdempotencyKey(),
                sweepRequest.getSweepRequestId(),
                sweepRequest.getUserUuid(),
                sweepRequest.getCardUserUuid(),
                sweepRequest.getPerformanceId(),
                sweepRequest.getPointLedgerId(),
                sweepRequest.getBaseMonth(),
                sweepRequest.getPointAmount(),
                sweepRequest.getKrwAmount(),
                sweepRequest.getEtfId(),
                sweepRequest.getRequestedAt()
        );
    }
}