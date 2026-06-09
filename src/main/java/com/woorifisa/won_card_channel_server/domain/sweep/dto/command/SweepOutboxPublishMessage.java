package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

import java.util.UUID;

public record SweepOutboxPublishMessage(
        Long outboxEventId,
        Long sweepRequestId,
        String eventId,
        String payload,
        String idempotencyKey,
        UUID cardUserUuid
) {
}
