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

    private static final String FAILED_STEP_AUTO_INVEST = "AUTO_INVEST";
    private static final String STATUS_NOT_PROVIDED = "NOT_PROVIDED";
    private static final String FAILED_REQUEST_STATUS = "FAILED";

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
                FAILED_STEP_AUTO_INVEST,
                STATUS_NOT_PROVIDED,
                STATUS_NOT_PROVIDED,
                item.failReason(),
                FAILED_REQUEST_STATUS.equals(item.requestStatus()),
                item.requestedAt(),
                item.completedAt(),
                item.updatedAt()
        );
    }
}
