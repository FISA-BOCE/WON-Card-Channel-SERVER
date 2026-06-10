package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepResultInbox;

import java.time.LocalDateTime;

public record AdminInboxEventItemResponse(
        Long inboxId,
        String systemType,
        Long sweepRequestId,
        String sourceEventId,
        String eventType,
        String processStatus,
        int retryCount,
        String lastErrorMessage,
        LocalDateTime receivedAt,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminInboxEventItemResponse from(SweepResultInbox inbox) {
        return new AdminInboxEventItemResponse(
                inbox.getInboxEventId(),
                "CARD",
                inbox.getSweepRequestId(),
                inbox.getSourceEventId(),
                inbox.getEventType().name(),
                inbox.getProcessStatus().name(),
                inbox.getRetryCount(),
                inbox.getLastErrorMessage(),
                inbox.getReceivedAt(),
                inbox.getProcessedAt(),
                inbox.getCreatedAt(),
                inbox.getUpdatedAt()
        );
    }
}
