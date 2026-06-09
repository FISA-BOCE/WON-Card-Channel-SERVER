package com.woorifisa.won_card_channel_server.domain.reward.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RewardErrorCode implements ErrorCode {

    INVALID_REWARD_LEDGER_TYPE(HttpStatus.BAD_REQUEST, "REWARD_400_001", "유효하지 않은 type 값입니다."),
    INVALID_REWARD_LEDGER_ID(HttpStatus.BAD_REQUEST, "REWARD_400_002", "유효하지 않은 리워드 내역 ID입니다."),

    REWARD_LEDGER_FORBIDDEN(HttpStatus.FORBIDDEN, "REWARD_403_001", "본인의 리워드 내역이 아닙니다."),

    REWARD_LEDGER_NOT_FOUND(HttpStatus.NOT_FOUND, "REWARD_404_001", "리워드 내역을 찾을 수 없습니다."),

    INVALID_REWARD_RESPONSE(HttpStatus.BAD_GATEWAY, "REWARD_502_001", "리워드 정보 응답 형식이 올바르지 않습니다."),
    REWARD_INFORMATION_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "REWARD_502_002", "리워드 정보를 불러오지 못했습니다.");

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
