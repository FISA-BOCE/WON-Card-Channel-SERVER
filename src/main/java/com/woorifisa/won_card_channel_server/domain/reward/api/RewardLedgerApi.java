package com.woorifisa.won_card_channel_server.domain.reward.api;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.service.RewardLedgerService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reward", description = "자동 투자 리워드 관련 API")
public class RewardLedgerApi {

    private final RewardLedgerService rewardLedgerService;

    @Operation(summary = "자동 투자 리워드 목록 조회", description = "자동 투자 된 리워드 목록 조회 페이지에서 사용되는 API입니다.")
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

    @Operation(summary = "자동 투자 리워드 목록 상세 조회", description = "자동 투자 된 리워드 목록 조회 상세 페이지에서 사용되는 API입니다.")
    @GetMapping("/api/cards/rewards/ledger/{pointLedgerId}")
    public ResponseEntity<ApiResponse<RewardLedgerDetailResponse>> getRewardLedger(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long pointLedgerId
    ) {
        RewardLedgerDetailResponse response = rewardLedgerService.getRewardLedgerDetail(authenticatedUser, pointLedgerId);

        return ResponseEntity
                .status(SuccessStatus.REWARD_LEDGER_DETAIL_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.REWARD_LEDGER_DETAIL_FOUND, response));
    }

}
