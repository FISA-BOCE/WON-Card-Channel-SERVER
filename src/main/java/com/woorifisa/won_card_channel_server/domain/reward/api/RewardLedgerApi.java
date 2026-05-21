package com.woorifisa.won_card_channel_server.domain.reward.api;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.service.RewardLedgerService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RewardLedgerApi {

    private final RewardLedgerService rewardLedgerService;

    @GetMapping("/api/cards/rewards/ledger")
    public ResponseEntity<ApiResponse<RewardLedgerResponse>> getRewardLedger(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String type
    ) {
        RewardLedgerResponse response = rewardLedgerService.getRewardLedger(authenticatedUser, type);

        return ResponseEntity
                .status(SuccessStatus.REWARD_LEDGER_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.REWARD_LEDGER_FOUND, response));
    }

}
