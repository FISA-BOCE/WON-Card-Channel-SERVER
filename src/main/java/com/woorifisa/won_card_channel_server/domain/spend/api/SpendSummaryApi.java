package com.woorifisa.won_card_channel_server.domain.spend.api;

import com.woorifisa.won_card_channel_server.domain.spend.dto.response.SpendCurrentAmountResponse;
import com.woorifisa.won_card_channel_server.domain.spend.service.SpendSummaryService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Spend", description = "카드 이용 금액 API")
public class SpendSummaryApi {

    private final SpendSummaryService spendSummaryService;

    @Operation(summary = "당월 이용 금액 조회", description = "당월 이용 금액과 적립률 구간, 예상 리워드를 조회합니다.")
    @GetMapping("/api/cards/spend-summary")
    public ResponseEntity<ApiResponse<SpendCurrentAmountResponse>> getSpendSummary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        SpendCurrentAmountResponse response = spendSummaryService.getSpendSummary(authenticatedUser);

        return ResponseEntity
                .status(SuccessStatus.CURRENT_SPEND_AMOUNT_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.CURRENT_SPEND_AMOUNT_FOUND, response));
    }
}
