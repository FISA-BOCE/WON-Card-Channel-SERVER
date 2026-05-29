package com.woorifisa.won_card_channel_server.domain.performance.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceErrorCode implements ErrorCode {

    INVALID_QUERY_MONTH(HttpStatus.BAD_REQUEST, "CARD_400_001", "유효하지 않은 조회 월입니다."),
    INVALID_PERFORMANCE_AMOUNT(HttpStatus.BAD_REQUEST, "CARD_400_002", "금액 형식이 올바르지 않습니다."),
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD_404_001", "실적 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    PerformanceErrorCode(HttpStatus httpStatus, String code, String message) {
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
