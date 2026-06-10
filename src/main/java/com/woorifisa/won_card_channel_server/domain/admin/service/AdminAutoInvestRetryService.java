package com.woorifisa.won_card_channel_server.domain.admin.service;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminAutoInvestRetryTargetItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminAutoInvestRetryTargetListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminAutoInvestRetryTargetSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAutoInvestRetryService {

    private final AdminSweepService adminSweepService;

    public AdminAutoInvestRetryTargetListResponse getRetryTargets(
            String baseMonth,
            UUID cardUserUuid,
            Long sweepRequestId,
            int page,
            int size
    ) {
        AdminSweepRequestListResponse failedSweepRequests = adminSweepService.getSweepRequests(
                "FAILED",
                baseMonth,
                cardUserUuid,
                sweepRequestId,
                page,
                size
        );

        return new AdminAutoInvestRetryTargetListResponse(
                new AdminAutoInvestRetryTargetSummaryResponse(
                        failedSweepRequests.totalCount(),
                        0,
                        0,
                        0
                ),
                failedSweepRequests.items()
                        .stream()
                        .map(AdminAutoInvestRetryTargetItemResponse::from)
                        .toList(),
                failedSweepRequests.page(),
                failedSweepRequests.size(),
                failedSweepRequests.totalCount(),
                failedSweepRequests.totalPages()
        );
    }
}
