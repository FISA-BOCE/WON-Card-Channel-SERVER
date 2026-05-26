package com.woorifisa.won_card_channel_server.domain.sweep.api;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.request.InternalSweepRequestCreateRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.SweepRequestCreateResponse;
import com.woorifisa.won_card_channel_server.domain.sweep.service.AutoSweepRequestService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/cards/sweep-requests")
@Tag(name = "Internal Sweep API", description = "내부 스윕 API")
public class InternalSweepRequestApi {

    private final AutoSweepRequestService autoSweepRequestService;

    @Operation(
            summary = "내부 단건 자동 스윕 요청 생성",
            description = "검증 완료된 자동 스윕 대상 정보를 받아 card_chn_sweep_request와 card_chn_sweep_outbox를 생성합니다. 실제 사용자용 API가 아니며, 자동 배치/연동 테스트용 내부 API입니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SweepRequestCreateResponse>> createSweepRequest(
            @Valid @RequestBody InternalSweepRequestCreateRequest request
    ) {
        SweepRequestCreateResponse response = autoSweepRequestService.create(request);

        return ResponseEntity
                .status(SuccessStatus.SWEEP_REQUEST_CREATED.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.SWEEP_REQUEST_CREATED, response));
    }
}
