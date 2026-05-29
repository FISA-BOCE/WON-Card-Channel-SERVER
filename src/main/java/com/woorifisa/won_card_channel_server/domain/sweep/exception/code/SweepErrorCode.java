package com.woorifisa.won_card_channel_server.domain.sweep.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SweepErrorCode implements ErrorCode {

    SWEEP_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "SWEEP_400_001", "스윕 요청 값이 올바르지 않습니다."),

    SWEEP_REWARD_LEDGER_FORBIDDEN(HttpStatus.FORBIDDEN, "SWEEP_403_001", "해당 리워드 원장에 대한 권한이 없습니다."),

    SWEEP_REWARD_LEDGER_NOT_FOUND(HttpStatus.NOT_FOUND, "SWEEP_404_001", "스윕 대상 리워드 원장을 찾을 수 없습니다."),
    SWEEP_OUTBOX_NOT_FOUND(HttpStatus.NOT_FOUND, "SWEEP_404_001", "스윕 Outbox 이벤트를 찾을 수 없습니다."),

    SWEEP_REWARD_LEDGER_NOT_ELIGIBLE(HttpStatus.UNPROCESSABLE_ENTITY, "SWEEP_422_001", "스윕할 수 없는 리워드 원장입니다."),

    SWEEP_ALREADY_REQUESTED(HttpStatus.CONFLICT, "SWEEP_409_001", "이미 스윕 요청된 포인트 원장입니다."),
    SWEEP_OUTBOX_INVALID_PUBLISH_STATE(HttpStatus.CONFLICT, "SWEEP_409_002", "스윕 Outbox 이벤트가 발행 가능한 상태가 아닙니다."),

    SWEEP_REQUEST_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SWEEP_500_001", "스윕 요청 저장 중 오류가 발생했습니다."),
    SWEEP_OUTBOX_INVALID_BATCH_SIZE(HttpStatus.INTERNAL_SERVER_ERROR, "SWEEP_500_002", "스윕 Outbox 발행 배치 설정값이 올바르지 않습니다."),

    SWEEP_OUTBOX_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "SWEEP_502_001", "스윕 요청 이벤트 생성에 실패했습니다."),
    SWEEP_CORE_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "SWEEP_502_002", "카드 계정계 스윕 응답 형식이 올바르지 않습니다."),
    SWEEP_CORE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "SWEEP_502_003", "카드 계정계 스윕 요청을 처리하지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SweepErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
