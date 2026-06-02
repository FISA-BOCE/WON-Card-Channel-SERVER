package com.woorifisa.won_card_channel_server.domain.card.api;

import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardApplicationCreateRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationCreateResponse;
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

    @Operation(summary = "카드 신청", description = "카드 발급과 최초 자동투자 설정을 함께 처리합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CardApplicationCreateResponse>> applyCard(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "호출 서비스 식별자", required = true)
            @RequestHeader("X-Service-ID") String serviceId,
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
