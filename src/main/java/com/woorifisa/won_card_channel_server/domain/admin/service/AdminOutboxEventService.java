package com.woorifisa.won_card_channel_server.domain.admin.service;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import com.woorifisa.won_card_channel_server.global.exception.code.CommonErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOutboxEventService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SweepOutboxRepository sweepOutboxRepository;

    public AdminOutboxEventListResponse getOutboxEvents(
            String systemType,
            String status,
            SweepEventType eventType,
            Long sweepRequestId,
            int page,
            int size
    ) {
        validateSystemType(systemType);

        OutboxPublishStatus publishStatus = mapStatus(status);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<SweepOutbox> outboxEvents = sweepOutboxRepository.findAdminOutboxEvents(
                publishStatus,
                eventType,
                sweepRequestId,
                null,
                null,
                pageable
        );

        return new AdminOutboxEventListResponse(
                getSummary(eventType, sweepRequestId),
                outboxEvents.getContent()
                        .stream()
                        .map(AdminOutboxEventItemResponse::from)
                        .toList(),
                outboxEvents.getNumber(),
                outboxEvents.getSize(),
                outboxEvents.getTotalElements(),
                outboxEvents.getTotalPages()
        );
    }

    public AdminOutboxEventItemResponse getOutboxEvent(Long outboxEventId) {
        SweepOutbox outbox = sweepOutboxRepository.findById(outboxEventId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        return AdminOutboxEventItemResponse.from(outbox);
    }

    public AdminOutboxEventSummaryResponse getSummary(
            SweepEventType eventType,
            Long sweepRequestId
    ) {
        long publishedCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.PUBLISHED,
                eventType,
                sweepRequestId,
                null,
                null
        );
        long failedCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.FAILED,
                eventType,
                sweepRequestId,
                null,
                null
        );
        long retryingCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.RETRY,
                eventType,
                sweepRequestId,
                null,
                null
        );
        long pendingCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.PENDING,
                eventType,
                sweepRequestId,
                null,
                null
        ) + sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.PROCESSING,
                eventType,
                sweepRequestId,
                null,
                null
        );

        return new AdminOutboxEventSummaryResponse(
                publishedCount + failedCount + retryingCount + pendingCount,
                publishedCount,
                failedCount,
                retryingCount,
                pendingCount
        );
    }

    private void validateSystemType(String systemType) {
        if (systemType == null || systemType.isBlank() || "ALL".equalsIgnoreCase(systemType)) {
            return;
        }

        if (!"CARD".equalsIgnoreCase(systemType)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private OutboxPublishStatus mapStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        return switch (normalizedStatus) {
            case "PENDING" -> OutboxPublishStatus.PENDING;
            case "PROCESSING" -> OutboxPublishStatus.PROCESSING;
            case "PUBLISHED" -> OutboxPublishStatus.PUBLISHED;
            case "RETRY", "RETRYING" -> OutboxPublishStatus.RETRY;
            case "FAILED" -> OutboxPublishStatus.FAILED;
            default -> throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        };
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
