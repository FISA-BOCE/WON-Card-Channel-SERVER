package com.woorifisa.won_card_channel_server.domain.sweep.external;

import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepBatchStartRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepBatchStartResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepResultRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepRequestResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepResultResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepCancelResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepCandidateResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepReservationResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "card-core", contextId = "cardCoreRewardSweepApi", url = "${internal.services.card-core.base-url}")
public interface CardCoreRewardSweepApi {

    @PostMapping("/internal/cards/rewards/ledger/{pointLedgerId}/sweep-request")
    ApiResponse<CardCoreSweepRequestResponse> requestSweep(
            @RequestHeader("X-Card-User-UUID") UUID cardUserUuid,
            @PathVariable("pointLedgerId") Long pointLedgerId
    );

    @GetMapping("/internal/cards/rewards/ledger/sweep-candidates")
    ApiResponse<CardCoreSweepCandidateResponse> getSweepCandidates(
            @RequestParam("baseMonth") String baseMonth
    );

    @PostMapping("/internal/cards/rewards/sweep-batches")
    ApiResponse<CardCoreSweepBatchStartResponse> startSweepBatch(
            @RequestBody CardCoreSweepBatchStartRequest request
    );

    @PostMapping("/internal/cards/rewards/sweep-batches/{batchExecutionId}/reservations")
    ApiResponse<CardCoreSweepReservationResponse> reserveSweepBatch(
            @PathVariable("batchExecutionId") Long batchExecutionId,
            @RequestParam("size") Integer size
    );

    @PostMapping("/internal/cards/rewards/ledger/{pointLedgerId}/sweep-request/cancel")
    ApiResponse<CardCoreSweepCancelResponse> cancelSweepRequest(
            @RequestHeader("X-Card-User-UUID") UUID cardUserUuid,
            @PathVariable("pointLedgerId") Long pointLedgerId
    );

    @PostMapping("/internal/cards/rewards/ledger/{pointLedgerId}/sweep-result")
    ApiResponse<CardCoreSweepResultResponse> applySweepResult(
            @RequestHeader("X-Card-User-UUID") UUID cardUserUuid,
            @PathVariable("pointLedgerId") Long pointLedgerId,
            @RequestBody CardCoreSweepResultRequest request
    );


}
