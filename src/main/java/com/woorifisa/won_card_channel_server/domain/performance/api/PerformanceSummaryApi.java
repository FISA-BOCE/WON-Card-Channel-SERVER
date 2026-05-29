package com.woorifisa.won_card_channel_server.domain.performance.api;

import com.woorifisa.won_card_channel_server.domain.performance.dto.response.PreviousPerformanceResponse;
import com.woorifisa.won_card_channel_server.domain.performance.service.PerformanceSummaryService;
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
@Tag(name = "Performance", description = "카드 실적 API")
public class PerformanceSummaryApi {

    private final PerformanceSummaryService performanceSummaryService;

    @Operation(summary = "전월 실적 조회", description = "전월 카드 이용 실적과 리워드 상태를 조회합니다.")
    @GetMapping("/api/cards/performance/monthly")
    public ResponseEntity<ApiResponse<PreviousPerformanceResponse>> getPreviousPerformance(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        PreviousPerformanceResponse response = performanceSummaryService.getPreviousPerformance(authenticatedUser);

        return ResponseEntity
                .status(SuccessStatus.PREVIOUS_PERFORMANCE_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.PREVIOUS_PERFORMANCE_FOUND, response));
    }
}
