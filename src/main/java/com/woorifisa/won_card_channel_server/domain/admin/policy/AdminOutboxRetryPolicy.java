package com.woorifisa.won_card_channel_server.domain.admin.policy;

import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminOutboxRetryPolicy {

    public boolean isRetryable(SweepOutbox outbox) {
        return outbox != null && outbox.isRetryRequestable() && isRetryableError(outbox.getLastErrorMessage());
    }

    public String getDisabledReason(SweepOutbox outbox) {
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

    private boolean isRetryableError(String message) {
        if (message == null || message.isBlank()) {
            return true;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
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

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
