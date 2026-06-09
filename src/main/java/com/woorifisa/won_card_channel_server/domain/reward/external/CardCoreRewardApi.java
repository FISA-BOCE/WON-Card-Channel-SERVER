package com.woorifisa.won_card_channel_server.domain.reward.external;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardGetCurrentResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "card-core", contextId = "cardCoreRewardApi", url = "${internal.services.card-core.base-url}")
public interface CardCoreRewardApi {

    @GetMapping("/internal/cards/rewards/ledger")
    ApiResponse<CardCoreRewardLedgerResponse> getRewardLedger(
            @RequestHeader("X-Card-User-UUID") UUID cardUserUuid,
            @RequestParam("type") String type
    );

    @GetMapping("/internal/cards/rewards/ledger/{pointLedgerId}")
    ApiResponse<CardCoreRewardLedgerDetailResponse> getRewardLedgerDetail(
            @RequestHeader("X-Card-User-UUID") UUID cardUserUuid,
            @PathVariable("pointLedgerId") Long pointLedgerId
    );

    @GetMapping("/internal/cards/rewards/monthly")
    ApiResponse<RewardGetCurrentResponse> getCurrentMonthReward(
            @RequestHeader("X-User-UUID") UUID userUuid
    );
}
