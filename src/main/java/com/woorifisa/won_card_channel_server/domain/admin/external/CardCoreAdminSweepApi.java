package com.woorifisa.won_card_channel_server.domain.admin.external;

import com.woorifisa.won_card_channel_server.domain.admin.external.dto.CardCoreAdminSweepRequestItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.external.dto.CardCoreAdminSweepRequestListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.external.dto.CardCoreAdminSweepRequestSummaryResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "card-core", contextId = "cardCoreAdminSweepApi", url = "${internal.services.card-core.base-url}")
public interface CardCoreAdminSweepApi {

    @GetMapping("/internal/admin/cards/sweep-requests")
    ApiResponse<CardCoreAdminSweepRequestListResponse> getSweepRequests(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "baseMonth", required = false) String baseMonth,
            @RequestParam(value = "cardUserUuid", required = false) UUID cardUserUuid,
            @RequestParam(value = "sweepRequestId", required = false) Long sweepRequestId,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    @GetMapping("/internal/admin/cards/sweep-requests/summary")
    ApiResponse<CardCoreAdminSweepRequestSummaryResponse> getSummary(
            @RequestParam(value = "baseMonth", required = false) String baseMonth,
            @RequestParam(value = "cardUserUuid", required = false) UUID cardUserUuid,
            @RequestParam(value = "sweepRequestId", required = false) Long sweepRequestId
    );

    @GetMapping("/internal/admin/cards/sweep-requests/{sweepRequestId}")
    ApiResponse<CardCoreAdminSweepRequestItemResponse> getSweepRequest(
            @PathVariable("sweepRequestId") Long sweepRequestId
    );
}
