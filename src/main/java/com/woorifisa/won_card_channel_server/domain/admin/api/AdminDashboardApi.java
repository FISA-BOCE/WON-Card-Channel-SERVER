package com.woorifisa.won_card_channel_server.domain.admin.api;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminDashboardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.admin.service.AdminDashboardService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard API", description = "관리자 대시보드 홈 화면의 월간 운영 요약 정보를 조회하는 API")
public class AdminDashboardApi {

    private final AdminDashboardService adminDashboardService;

    @Operation(
            summary = "대시보드 홈 월간 요약 조회",
            description = "기준월의 투자 전환 요청, Outbox 이벤트, Inbox 이벤트 상태별 요약과 최근 실패 항목을 조회합니다. baseMonth를 생략하면 현재 월 기준으로 조회합니다."
    )
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getSummary(
            @RequestParam(required = false) String baseMonth
    ) {
        AdminDashboardSummaryResponse response = adminDashboardService.getSummary(baseMonth);

        return ResponseEntity
                .status(SuccessStatus.ADMIN_DASHBOARD_SUMMARY_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.ADMIN_DASHBOARD_SUMMARY_FOUND, response));
    }
}
