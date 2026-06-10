package com.woorifisa.won_card_channel_server.domain.admin.api;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminAutoInvestRetryTargetListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.service.AdminAutoInvestRetryService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auto-invest")
@Tag(name = "Admin Auto Invest Retry API", description = "관리자 대시보드의 자동투자 재처리 대상을 조회하는 API")
public class AdminAutoInvestRetryApi {

    private final AdminAutoInvestRetryService adminAutoInvestRetryService;

    @Operation(
            summary = "자동투자 재처리 대상 조회",
            description = "Card Core의 최종 스윕 상태가 FAILED인 투자 전환 요청을 기준으로 자동투자 재처리 후보 목록을 조회합니다. 실제 재처리 실행은 포함하지 않습니다."
    )
    @GetMapping("/retry-targets")
    public ResponseEntity<ApiResponse<AdminAutoInvestRetryTargetListResponse>> getRetryTargets(
            @RequestParam(required = false) String baseMonth,
            @RequestParam(required = false) UUID cardUserUuid,
            @RequestParam(required = false) Long sweepRequestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAutoInvestRetryTargetListResponse response = adminAutoInvestRetryService.getRetryTargets(
                baseMonth,
                cardUserUuid,
                sweepRequestId,
                page,
                size
        );

        return ResponseEntity
                .status(SuccessStatus.ADMIN_AUTO_INVEST_RETRY_TARGETS_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.ADMIN_AUTO_INVEST_RETRY_TARGETS_FOUND, response));
    }
}
