package com.woorifisa.won_card_channel_server.domain.sweep.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SweepErrorCode implements ErrorCode {

    SWEEP_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "SWEEP_400_001", "스윕 요청 값이 올바르지 않습니다."),

    SWEEP_ALREADY_REQUESTED(HttpStatus.CONFLICT, "SWEEP_409_001", "이미 스윕 요청된 포인트 원장입니다."),

    SWEEP_OUTBOX_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "SWEEP_502_001", "스윕 요청 이벤트 생성에 실패했습니다.");

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
