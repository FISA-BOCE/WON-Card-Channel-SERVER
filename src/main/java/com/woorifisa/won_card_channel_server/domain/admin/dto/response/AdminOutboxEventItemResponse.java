package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

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

    public static AdminOutboxEventItemResponse from(SweepOutbox outbox) {
        return new AdminOutboxEventItemResponse(
                outbox.getOutboxEventId(),
                "CARD",
                outbox.getSweepRequestId(),
                outbox.getEventType().name(),
                mapPublishStatus(outbox.getPublishStatus()),
                outbox.getRetryCount(),
                outbox.getLastErrorMessage(),
                isRetryable(outbox),
                retryDisabledReason(outbox),
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

    private static boolean isRetryable(SweepOutbox outbox) {
        return outbox != null && outbox.isRetryRequestable() && isRetryableError(outbox.getLastErrorMessage());
    }

    private static String retryDisabledReason(SweepOutbox outbox) {
        if (outbox == null || outbox.getPublishStatus() == null) {
            return "이벤트 상태를 확인할 수 없습니다.";
        }

        return switch (outbox.getPublishStatus()) {
            case PUBLISHED -> "이미 발행 완료된 이벤트는 재처리할 수 없습니다.";
            case PENDING -> "발행 대기 중인 이벤트는 재처리할 수 없습니다.";
            case PROCESSING -> "발행 처리 중인 이벤트는 재처리할 수 없습니다.";
            case RETRY, FAILED -> isRetryableError(outbox.getLastErrorMessage())
                    ? null
                    : "이 오류는 단순 재발행으로 복구하기 어렵습니다.";
        };
    }

    private static boolean isRetryableError(String message) {
        if (message == null || message.isBlank()) {
            return true;
        }

        String normalized = message.toLowerCase();
        return containsAny(
                normalized,
                "sqs",
                "queue",
                "does not exist",
                "timeout",
                "timed out",
                "connection",
                "connect",
                "endpoint",
                "credential",
                "throttl",
                "too many requests",
                "service unavailable",
                "internalerror",
                "sdkclientexception",
                "unable to execute http request"
        ) && !containsAny(
                normalized,
                "invalid event",
                "invalid payload",
                "invalid request",
                "missing",
                "null",
                "not active",
                "not available",
                "account",
                "etf",
                "amount"
        );
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
