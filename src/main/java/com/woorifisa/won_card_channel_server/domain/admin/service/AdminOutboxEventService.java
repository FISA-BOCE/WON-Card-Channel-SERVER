package com.woorifisa.won_card_channel_server.domain.admin.service;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.admin.policy.AdminOutboxRetryPolicy;
import com.woorifisa.won_card_channel_server.domain.admin.support.AdminRequestSupport;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
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

    private final SweepOutboxRepository sweepOutboxRepository;
    private final AdminOutboxRetryPolicy retryPolicy;

    public AdminOutboxEventListResponse getOutboxEvents(
            String systemType,
            String status,
            SweepEventType eventType,
            Long sweepRequestId,
            int page,
            int size
    ) {
        AdminRequestSupport.validateCardSystemType(systemType);

        OutboxPublishStatus publishStatus = mapStatus(status);
        Pageable pageable = PageRequest.of(
                AdminRequestSupport.normalizePage(page),
                AdminRequestSupport.normalizeSize(size)
        );
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
                        .map(outbox -> AdminOutboxEventItemResponse.from(outbox, retryPolicy))
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

        return AdminOutboxEventItemResponse.from(outbox, retryPolicy);
    }

    @Transactional
    public AdminOutboxEventItemResponse retryOutboxEvent(Long outboxEventId) {
        SweepOutbox outbox = sweepOutboxRepository.findById(outboxEventId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (!retryPolicy.isRetryable(outbox)) {
            throw new BusinessException(
                    SweepErrorCode.SWEEP_OUTBOX_RETRY_NOT_ALLOWED,
                    retryPolicy.getDisabledReason(outbox)
            );
        }

        outbox.markRetryRequested();
        return AdminOutboxEventItemResponse.from(outbox, retryPolicy);
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

    private OutboxPublishStatus mapStatus(String status) {
        if (AdminRequestSupport.isAll(status)) {
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
}
