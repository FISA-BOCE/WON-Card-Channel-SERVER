package com.woorifisa.won_card_channel_server.domain.reward.exception;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RewardErrorCode implements ErrorCode {

    INVALID_REWARD_LEDGER_TYPE(HttpStatus.BAD_REQUEST, "REWARD_400_001", "유효하지 않은 type 값입니다."),

    REWARD_LEDGER_NOT_FOUND(HttpStatus.NOT_FOUND, "REWARD_404_001", "리워드 내역을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    RewardErrorCode(HttpStatus httpStatus, String code, String message) {
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
