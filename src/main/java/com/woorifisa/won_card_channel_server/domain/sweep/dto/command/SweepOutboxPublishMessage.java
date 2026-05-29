package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

public record SweepOutboxPublishMessage(
        Long outboxEventId,
        Long sweepRequestId,
        String eventId,
        String payload,
        String idempotencyKey
) {
}
