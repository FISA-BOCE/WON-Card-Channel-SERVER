package com.woorifisa.won_card_channel_server.domain.sweep.external;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.CardCoreSweepRequestResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepCandidateResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "card-core", contextId = "cardCoreRewardSweepApi", url = "${internal.card-core.url}")
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
}
