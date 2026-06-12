package com.woorifisa.won_card_channel_server.domain.admin.api;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminSweepRequestListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.service.AdminSweepService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sweep-requests")
@Tag(name = "Admin Sweep API", description = "관리자 대시보드의 투자 전환 요청 목록을 조회하는 API")
public class AdminSweepApi {

    private final AdminSweepService adminSweepService;

    @Operation(
            summary = "투자 전환 요청 목록 조회",
            description = "관리자 화면의 투자 전환 요청 탭에서 사용할 목록을 조회합니다. Card Core의 최종 스윕 상태를 조회한 뒤 Card Channel의 스윕 요청 데이터로 사용자 UUID, 원화 금액, ETF ID, 요청/완료 시각을 보강합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<AdminSweepRequestListResponse>> getSweepRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String baseMonth,
            @RequestParam(required = false) UUID cardUserUuid,
            @RequestParam(required = false) Long sweepRequestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminSweepRequestListResponse response = adminSweepService.getSweepRequests(
                status,
                baseMonth,
                cardUserUuid,
                sweepRequestId,
                page,
                size
        );

        return ResponseEntity
                .status(SuccessStatus.ADMIN_SWEEP_REQUESTS_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.ADMIN_SWEEP_REQUESTS_FOUND, response));
    }

    @Operation(
            summary = "투자 전환 요청 상세 조회",
            description = "관리자 화면에서 특정 투자 전환 요청의 상세 상태를 조회합니다. Card Core의 원장 상태와 Card Channel의 요청 정보를 조합해 반환합니다."
    )
    @GetMapping("/{sweepRequestId}")
    public ResponseEntity<ApiResponse<AdminSweepRequestItemResponse>> getSweepRequest(
            @PathVariable Long sweepRequestId
    ) {
        AdminSweepRequestItemResponse response = adminSweepService.getSweepRequest(sweepRequestId);

        return ResponseEntity
                .status(SuccessStatus.ADMIN_SWEEP_REQUEST_DETAIL_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.ADMIN_SWEEP_REQUEST_DETAIL_FOUND, response));
    }
}
