package com.woorifisa.won_card_channel_server.domain.admin.dto.response;

import com.woorifisa.won_card_channel_server.domain.admin.external.dto.CardCoreAdminSweepRequestItemResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepProcessStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminSweepRequestItemResponse(
        Long sweepRequestId,
        Long pointLedgerId,
        UUID userUuid,
        UUID cardUserUuid,
        String baseMonth,
        Long pointAmount,
        Long krwAmount,
        Long etfId,
        String ticker,
        String requestStatus,
        String failReason,
        LocalDateTime requestedAt,
        LocalDateTime sentAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {

    public static AdminSweepRequestItemResponse from(
            CardCoreAdminSweepRequestItemResponse coreItem,
            Sweep sweep
    ) {
        return new AdminSweepRequestItemResponse(
                resolveSweepRequestId(coreItem, sweep),
                coreItem.pointLedgerId(),
                sweep == null ? coreItem.cardUserUuid() : sweep.getUserUuid(),
                coreItem.cardUserUuid(),
                coreItem.baseMonth(),
                coreItem.pointAmount(),
                sweep == null ? coreItem.pointAmount() : sweep.getKrwAmount(),
                sweep == null ? null : sweep.getEtfId(),
                null,
                resolveStatus(coreItem.sweepStatus(), sweep),
                resolveFailReason(coreItem, sweep),
                sweep == null ? coreItem.requestedAt() : sweep.getRequestedAt(),
                sweep == null ? coreItem.requestedAt() : sweep.getSentAt(),
                resolveCompletedAt(coreItem, sweep),
                sweep == null ? coreItem.updatedAt() : sweep.getUpdatedAt()
        );
    }

    private static Long resolveSweepRequestId(CardCoreAdminSweepRequestItemResponse coreItem, Sweep sweep) {
        if (sweep != null) {
            return sweep.getSweepRequestId();
        }

        return coreItem.sweepRequestId();
    }

    private static String resolveStatus(String coreStatus, Sweep sweep) {
        if (sweep != null) {
            return mapSweepProcessStatus(sweep.getRequestStatus());
        }

        return mapCoreSweepStatus(coreStatus);
    }

    private static String resolveFailReason(CardCoreAdminSweepRequestItemResponse coreItem, Sweep sweep) {
        if (sweep != null && sweep.getFailReason() != null && !sweep.getFailReason().isBlank()) {
            return sweep.getFailReason();
        }

        return coreItem.failureMessage();
    }

    private static LocalDateTime resolveCompletedAt(CardCoreAdminSweepRequestItemResponse coreItem, Sweep sweep) {
        if (sweep != null && sweep.getCompletedAt() != null) {
            return sweep.getCompletedAt();
        }

        return coreItem.completedAt();
    }

    private static String mapSweepProcessStatus(SweepProcessStatus status) {
        if (status == null) {
            return "CREATED";
        }

        return switch (status) {
            case PENDING_PUBLISH -> "CREATED";
            case SENT -> "PROCESSING";
            case SUCCEEDED -> "COMPLETED";
            case FAILED -> "FAILED";
        };
    }

    private static String mapCoreSweepStatus(String status) {
        if (status == null) {
            return "CREATED";
        }

        return switch (status) {
            case "NONE" -> "CREATED";
            case "REQUESTED" -> "PROCESSING";
            case "COMPLETED" -> "COMPLETED";
            case "FAILED" -> "FAILED";
            default -> status;
        };
    }
}
