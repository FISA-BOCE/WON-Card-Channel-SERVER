package com.woorifisa.won_card_channel_server.domain.card.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CardErrorCode implements ErrorCode {

    CARD_APPLICATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "CARD_400_001", "입력값 형식이 올바르지 않습니다."),
    CARD_APPLICATION_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "CARD_400_002", "필수 약관에 동의하지 않았습니다."),
    CARD_INVEST_LINK_REQUIRED(HttpStatus.BAD_REQUEST, "CARD_400_003", "증권 서비스 연결이 필요합니다."),

    CARD_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD_404_001", "카드 사용자 정보를 찾을 수 없습니다."),

    CARD_ALREADY_EXISTS(HttpStatus.CONFLICT, "CARD_409_001", "이미 발급된 카드가 존재합니다."),
    CARD_USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "CARD_409_002", "이미 등록된 카드 고객입니다."),
    CARD_CONSTRAINT_CONFLICT(HttpStatus.CONFLICT, "CARD_409_003", "제약사항 충돌"),

    CARD_ISSUANCE_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "CARD_422_001", "해당 고객은 발급이 불가합니다."),

    INVALID_CARD_RESPONSE(HttpStatus.BAD_GATEWAY, "CARD_502_001", "카드 정보 응답 형식이 올바르지 않습니다."),
    CARD_INFORMATION_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "CARD_502_002", "카드 정보를 불러오지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CardErrorCode(HttpStatus httpStatus, String code, String message) {
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
