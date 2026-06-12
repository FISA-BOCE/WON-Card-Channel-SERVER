package com.woorifisa.won_card_channel_server.domain.admin.api;

import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventItemResponse;
import com.woorifisa.won_card_channel_server.domain.admin.dto.response.AdminOutboxEventListResponse;
import com.woorifisa.won_card_channel_server.domain.admin.service.AdminOutboxEventService;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/outbox-events")
@Tag(name = "Admin Outbox Event API", description = "관리자 대시보드의 Card Channel Outbox 이벤트를 조회하는 API")
public class AdminOutboxEventApi {

    private final AdminOutboxEventService adminOutboxEventService;

    @Operation(
            summary = "Outbox 이벤트 목록 조회",
            description = "Card Channel의 card_chn_sweep_outbox 테이블 기준으로 외부 발행 이벤트 목록과 상태별 요약 정보를 조회합니다. 시스템, 발행 상태, 이벤트 타입, 스윕 요청 ID로 필터링할 수 있으며 페이지네이션을 지원합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<AdminOutboxEventListResponse>> getOutboxEvents(
            @RequestParam(required = false) String systemType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) SweepEventType eventType,
            @RequestParam(required = false) Long sweepRequestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminOutboxEventListResponse response = adminOutboxEventService.getOutboxEvents(
                systemType,
                status,
                eventType,
                sweepRequestId,
                page,
                size
        );

        return ResponseEntity
                .status(SuccessStatus.ADMIN_OUTBOX_EVENTS_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.ADMIN_OUTBOX_EVENTS_FOUND, response));
    }

    @Operation(
            summary = "Outbox 이벤트 상세 조회",
            description = "Outbox 이벤트 ID로 Card Channel의 이벤트 발행 상태, 재시도 횟수, 마지막 오류 메시지, 발행/생성 시각을 조회합니다."
    )
    @GetMapping("/{outboxEventId}")
    public ResponseEntity<ApiResponse<AdminOutboxEventItemResponse>> getOutboxEvent(
            @PathVariable Long outboxEventId
    ) {
        AdminOutboxEventItemResponse response = adminOutboxEventService.getOutboxEvent(outboxEventId);

        return ResponseEntity
                .status(SuccessStatus.ADMIN_OUTBOX_EVENT_DETAIL_FOUND.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.ADMIN_OUTBOX_EVENT_DETAIL_FOUND, response));
    }

    @Operation(
            summary = "Outbox 이벤트 재처리 요청",
            description = "SQS 발행 실패 등 운영/전달 실패로 멈춘 Outbox 이벤트를 RETRY 상태로 되돌립니다. 새 스윕 요청을 생성하지 않고 기존 Publisher 스케줄러가 재발행하도록 nextRetryAt을 현재 시각으로 갱신합니다."
    )
    @PostMapping("/{outboxEventId}/retry")
    public ResponseEntity<ApiResponse<AdminOutboxEventItemResponse>> retryOutboxEvent(
            @PathVariable Long outboxEventId
    ) {
        AdminOutboxEventItemResponse response = adminOutboxEventService.retryOutboxEvent(outboxEventId);

        return ResponseEntity
                .status(SuccessStatus.ADMIN_OUTBOX_EVENT_RETRY_REQUESTED.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.ADMIN_OUTBOX_EVENT_RETRY_REQUESTED, response));
    }
}
