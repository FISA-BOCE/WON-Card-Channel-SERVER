package com.woorifisa.won_card_channel_server.domain.admin.service;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminDashboardKpiResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminDashboardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminInboxEventItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminInboxEventSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.InboxProcessStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepResultInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int RECENT_FAILED_LIMIT = 5;

    private final AdminSweepService adminSweepService;
    private final SweepOutboxRepository sweepOutboxRepository;
    private final SweepResultInboxRepository sweepResultInboxRepository;

    public AdminDashboardSummaryResponse getSummary(String baseMonth) {
        YearMonth yearMonth = resolveBaseMonth(baseMonth);
        String normalizedBaseMonth = yearMonth.toString();
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        AdminSweepRequestListResponse sweepRequests = adminSweepService.getSweepRequests(
                null,
                normalizedBaseMonth,
                null,
                null,
                0,
                RECENT_FAILED_LIMIT
        );
        AdminSweepRequestListResponse failedSweepRequests = adminSweepService.getSweepRequests(
                "FAILED",
                normalizedBaseMonth,
                null,
                null,
                0,
                RECENT_FAILED_LIMIT
        );

        AdminSweepRequestSummaryResponse sweepSummary = sweepRequests.summary();
        AdminOutboxEventSummaryResponse outboxSummary = getOutboxSummary(monthStart, nextMonthStart);
        AdminInboxEventSummaryResponse inboxSummary = getInboxSummary(monthStart, nextMonthStart);

        return new AdminDashboardSummaryResponse(
                normalizedBaseMonth,
                new AdminDashboardKpiResponse(
                        sweepSummary.totalCount(),
                        sweepSummary.completedCount(),
                        sweepSummary.failedCount(),
                        outboxSummary.failedCount(),
                        inboxSummary.failedCount()
                ),
                sweepSummary,
                outboxSummary,
                inboxSummary,
                failedSweepRequests.items(),
                getRecentFailedOutboxEvents(monthStart, nextMonthStart),
                getRecentFailedInboxEvents(monthStart, nextMonthStart)
        );
    }

    private AdminOutboxEventSummaryResponse getOutboxSummary(
            LocalDateTime monthStart,
            LocalDateTime nextMonthStart
    ) {
        long publishedCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.PUBLISHED,
                null,
                null,
                monthStart,
                nextMonthStart
        );
        long failedCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.FAILED,
                null,
                null,
                monthStart,
                nextMonthStart
        );
        long retryingCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.RETRY,
                null,
                null,
                monthStart,
                nextMonthStart
        );
        long pendingCount = sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.PENDING,
                null,
                null,
                monthStart,
                nextMonthStart
        ) + sweepOutboxRepository.countAdminOutboxEvents(
                OutboxPublishStatus.PROCESSING,
                null,
                null,
                monthStart,
                nextMonthStart
        );

        return new AdminOutboxEventSummaryResponse(
                publishedCount + failedCount + retryingCount + pendingCount,
                publishedCount,
                failedCount,
                retryingCount,
                pendingCount
        );
    }

    private AdminInboxEventSummaryResponse getInboxSummary(
            LocalDateTime monthStart,
            LocalDateTime nextMonthStart
    ) {
        long processedCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.PROCESSED,
                null,
                null,
                monthStart,
                nextMonthStart
        );
        long failedCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.FAILED,
                null,
                null,
                monthStart,
                nextMonthStart
        );
        long processingCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.PROCESSING,
                null,
                null,
                monthStart,
                nextMonthStart
        );
        long receivedCount = sweepResultInboxRepository.countAdminInboxEvents(
                InboxProcessStatus.RECEIVED,
                null,
                null,
                monthStart,
                nextMonthStart
        );

        return new AdminInboxEventSummaryResponse(
                processedCount + failedCount + processingCount + receivedCount,
                processedCount,
                failedCount,
                processingCount,
                receivedCount
        );
    }

    private java.util.List<AdminOutboxEventItemResponse> getRecentFailedOutboxEvents(
            LocalDateTime monthStart,
            LocalDateTime nextMonthStart
    ) {
        return sweepOutboxRepository.findAdminOutboxEvents(
                        OutboxPublishStatus.FAILED,
                        null,
                        null,
                        monthStart,
                        nextMonthStart,
                        PageRequest.of(0, RECENT_FAILED_LIMIT)
                )
                .getContent()
                .stream()
                .map(AdminOutboxEventItemResponse::from)
                .toList();
    }

    private java.util.List<AdminInboxEventItemResponse> getRecentFailedInboxEvents(
            LocalDateTime monthStart,
            LocalDateTime nextMonthStart
    ) {
        return sweepResultInboxRepository.findAdminInboxEvents(
                        InboxProcessStatus.FAILED,
                        null,
                        null,
                        monthStart,
                        nextMonthStart,
                        PageRequest.of(0, RECENT_FAILED_LIMIT)
                )
                .getContent()
                .stream()
                .map(AdminInboxEventItemResponse::from)
                .toList();
    }

    private YearMonth resolveBaseMonth(String baseMonth) {
        if (baseMonth == null || baseMonth.isBlank()) {
            return YearMonth.from(LocalDate.now());
        }

        return YearMonth.parse(baseMonth);
    }
}
