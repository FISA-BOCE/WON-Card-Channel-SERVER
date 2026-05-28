package com.woorifisa.won_card_channel_server.domain.spend.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SpendErrorCode implements ErrorCode {

    CURRENT_SPEND_AMOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD_404_001", "이용 금액 정보를 찾을 수 없습니다."),
    INVALID_SPEND_RESPONSE(HttpStatus.BAD_GATEWAY, "SPEND_502_001", "이용 금액 응답 형식이 올바르지 않습니다."),
    SPEND_INFORMATION_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "SPEND_502_002", "이용 금액 정보를 불러오지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SpendErrorCode(HttpStatus httpStatus, String code, String message) {
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
