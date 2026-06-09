package com.woorifisa.won_card_channel_server.domain.card.api;

import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationInvestAccountsResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardApplicationCreateRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationCreateResponse;
import com.woorifisa.won_card_channel_server.domain.card.service.CardApplicationInvestAccountService;
import com.woorifisa.won_card_channel_server.domain.card.service.CardApplicationService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards/applications")
@Tag(name = "Card", description = "카드 신청 API")
public class CardApplicationApi {

    private final CardApplicationService cardApplicationService;
    private final CardApplicationInvestAccountService cardApplicationInvestAccountService;

    @Operation(summary = "카드 신청용 증권 계좌 목록 조회", description = "카드 신청 화면에서 사용할 보유 증권 계좌 목록을 조회합니다.")
    @GetMapping("/invest-accounts")
    public ResponseEntity<ApiResponse<CardApplicationInvestAccountsResponse>> getInvestAccounts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        CardApplicationInvestAccountsResponse response =
                cardApplicationInvestAccountService.getInvestAccounts(authenticatedUser);

        return ResponseEntity
                .status(SuccessStatus.CARD_INVEST_ACCOUNTS_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.CARD_INVEST_ACCOUNTS_FOUND, response));
    }

    @Operation(summary = "카드 신청", description = "카드 발급과 최초 자동투자 설정을 함께 처리합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CardApplicationCreateResponse>> applyCard(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId,
            @Valid @RequestBody CardApplicationCreateRequest request
    ) {
        CardApplicationCreateResponse response = cardApplicationService.applyCard(authenticatedUser, request);
        return ResponseEntity
                .status(SuccessStatus.CARD_APPLICATION_CREATED.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.CARD_APPLICATION_CREATED, response));
    }
}
