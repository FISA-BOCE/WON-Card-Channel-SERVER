package com.woorifisa.won_card_channel_server.domain.card.api;

import com.woorifisa.won_card_channel_server.domain.card.dto.response.NoCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.service.CardSummaryService;
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
@Tag(name = "Card", description = "카드 관련 API")
public class CardApi {

    private final CardSummaryService cardSummaryService;

    @Operation(summary = "카드 목록 조회", description = "보유 카드 여부와 카드 사용 요약 정보를 조회합니다.")
    @GetMapping("/api/cards")
    public ResponseEntity<ApiResponse<?>> getCards(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        Object response = cardSummaryService.getCards(authenticatedUser);
        SuccessStatus successStatus = response instanceof NoCardSummaryResponse
                ? SuccessStatus.CARD_SUMMARY_NOT_FOUND
                : SuccessStatus.CARD_SUMMARY_FOUND;

        return ResponseEntity
                .status(successStatus.getHttpStatus())
                .body(ApiResponse.of(successStatus, response));
    }
}
