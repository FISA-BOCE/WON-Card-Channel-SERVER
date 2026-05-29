package com.woorifisa.won_card_channel_server.domain.autoinvest.api;

import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.request.AutoInvestSubscriptionChangeRequest;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionChangeResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionDetailResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.AutoInvestSubscriptionService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards/auto-invest")
@Tag(name = "AutoInvest", description = "카드 리워드 자동투자 설정 API")
public class AutoInvestSubscriptionApi {

    private final AutoInvestSubscriptionService autoInvestSubscriptionService;

    @Operation(summary = "자동투자 설정 조회", description = "현재 선택된 자동투자 ETF를 조회합니다.")
    @GetMapping("/subscriptions/{subscriptionUuid}")
    public ResponseEntity<ApiResponse<AutoInvestSubscriptionDetailResponse>> getSubscription(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "호출 서비스 식별자", required = true)
            @RequestHeader("X-Service-ID") String serviceId,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId,
            @Parameter(description = "현재 카드 식별값")
            @PathVariable UUID subscriptionUuid
    ) {
        AutoInvestSubscriptionDetailResponse response =
                autoInvestSubscriptionService.getSubscription(authenticatedUser, subscriptionUuid);

        return ResponseEntity
                .status(SuccessStatus.AUTO_INVEST_SUBSCRIPTION_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.AUTO_INVEST_SUBSCRIPTION_FOUND, response));
    }

    @Operation(summary = "자동투자 ETF 변경", description = "현재 자동투자 ETF를 변경합니다.")
    @PatchMapping("/subscriptions/{subscriptionUuid}")
    public ResponseEntity<ApiResponse<AutoInvestSubscriptionChangeResponse>> changeSubscription(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "호출 서비스 식별자", required = true)
            @RequestHeader("X-Service-ID") String serviceId,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId,
            @Parameter(description = "현재 카드 식별값")
            @PathVariable UUID subscriptionUuid,
            @Valid @RequestBody AutoInvestSubscriptionChangeRequest request
    ) {
        AutoInvestSubscriptionChangeResponse response =
                autoInvestSubscriptionService.changeSubscription(authenticatedUser, subscriptionUuid, request);

        return ResponseEntity
                .status(SuccessStatus.AUTO_INVEST_SUBSCRIPTION_CHANGED.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.AUTO_INVEST_SUBSCRIPTION_CHANGED, response));
    }
}
