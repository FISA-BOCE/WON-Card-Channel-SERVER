package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAutoInvestRetryTargetItemResponse(
        Long executionId,
        Long sweepRequestId,
        Long pointLedgerId,
        UUID userUuid,
        UUID cardUserUuid,
        String baseMonth,
        Long pointAmount,
        Long krwAmount,
        Long etfId,
        String ticker,
        String failedStep,
        String exchangeStatus,
        String orderStatus,
        String failReason,
        boolean retryable,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {

    public static AdminAutoInvestRetryTargetItemResponse from(AdminSweepRequestItemResponse item) {
        return new AdminAutoInvestRetryTargetItemResponse(
                item.sweepRequestId(),
                item.sweepRequestId(),
                item.pointLedgerId(),
                item.userUuid(),
                item.cardUserUuid(),
                item.baseMonth(),
                item.pointAmount(),
                item.krwAmount(),
                item.etfId(),
                item.ticker(),
                "AUTO_INVEST",
                "FAILED",
                "UNKNOWN",
                item.failReason(),
                "FAILED".equals(item.requestStatus()),
                item.requestedAt(),
                item.completedAt(),
                item.updatedAt()
        );
    }
}
