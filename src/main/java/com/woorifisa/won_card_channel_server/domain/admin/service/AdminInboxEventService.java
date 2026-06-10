package com.woorifisa.won_card_channel_server.domain.admin.service;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminInboxEventItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminInboxEventListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminInboxEventSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepResultInbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.InboxProcessStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepResultInboxRepository;
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
public class AdminInboxEventService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SweepResultInboxRepository sweepResultInboxRepository;

    public AdminInboxEventListResponse getInboxEvents(
            String systemType,
            String status,
            SweepEventType eventType,
            Long sweepRequestId,
            int page,
            int size
    ) {
        validateSystemType(systemType);

        InboxProcessStatus processStatus = mapStatus(status);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<SweepResultInbox> inboxEvents = sweepResultInboxRepository.findAdminInboxEvents(
                processStatus,
                eventType,
                sweepRequestId,
                pageable
        );

        return new AdminInboxEventListResponse(
                getSummary(eventType, sweepRequestId),
                inboxEvents.getContent()
                        .stream()
                        .map(AdminInboxEventItemResponse::from)
                        .toList(),
                inboxEvents.getNumber(),
                inboxEvents.getSize(),
                inboxEvents.getTotalElements(),
                inboxEvents.getTotalPages()
        );
    }

    public AdminInboxEventItemResponse getInboxEvent(Long inboxEventId) {
        SweepResultInbox inbox = sweepResultInboxRepository.findById(inboxEventId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        return AdminInboxEventItemResponse.from(inbox);
    }

    public AdminInboxEventSummaryResponse getSummary(
            SweepEventType eventType,
            Long sweepRequestId
    ) {
        long processedCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.PROCESSED,
                eventType,
                sweepRequestId
        );
        long failedCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.FAILED,
                eventType,
                sweepRequestId
        );
        long processingCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.PROCESSING,
                eventType,
                sweepRequestId
        );
        long receivedCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.RECEIVED,
                eventType,
                sweepRequestId
        );

        return new AdminInboxEventSummaryResponse(
                processedCount + failedCount + processingCount + receivedCount,
                processedCount,
                failedCount,
                processingCount,
                receivedCount
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

    private InboxProcessStatus mapStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        return switch (normalizedStatus) {
            case "RECEIVED" -> InboxProcessStatus.RECEIVED;
            case "PROCESSING" -> InboxProcessStatus.PROCESSING;
            case "PROCESSED", "COMPLETED" -> InboxProcessStatus.PROCESSED;
            case "FAILED" -> InboxProcessStatus.FAILED;
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
