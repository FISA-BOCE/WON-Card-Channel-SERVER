package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import com.woorifisa.won_card_channel_server.domain.admin.policy.AdminOutboxRetryPolicy;
import com.woorifisa.won_card_channel_server.domain.admin.support.AdminSystemType;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;

import java.time.LocalDateTime;

public record AdminOutboxEventItemResponse(
        Long outboxId,
        String systemType,
        Long sweepRequestId,
        String eventType,
        String publishStatus,
        int retryCount,
        String lastErrorMessage,
        boolean retryable,
        String retryDisabledReason,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminOutboxEventItemResponse from(
            SweepOutbox outbox,
            AdminOutboxRetryPolicy retryPolicy
    ) {
        return new AdminOutboxEventItemResponse(
                outbox.getOutboxEventId(),
                AdminSystemType.CARD,
                outbox.getSweepRequestId(),
                outbox.getEventType().name(),
                mapPublishStatus(outbox.getPublishStatus()),
                outbox.getRetryCount(),
                outbox.getLastErrorMessage(),
                retryPolicy.isRetryable(outbox),
                retryPolicy.getDisabledReason(outbox),
                outbox.getPublishedAt(),
                outbox.getCreatedAt(),
                outbox.getUpdatedAt()
        );
    }

    private static String mapPublishStatus(OutboxPublishStatus status) {
        if (status == null) {
            return "PENDING";
        }

        return switch (status) {
            case PENDING, PROCESSING -> "PENDING";
            case PUBLISHED -> "PUBLISHED";
            case RETRY -> "RETRYING";
            case FAILED -> "FAILED";
        };
    }
}
