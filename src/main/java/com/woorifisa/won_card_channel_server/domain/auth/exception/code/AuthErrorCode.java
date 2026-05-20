package com.woorifisa.won_card_channel_server.domain.auth.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "AUTH_400_001", "입력값 형식이 올바르지 않습니다."),
    EMPTY_UPDATE_REQUEST(HttpStatus.BAD_REQUEST, "AUTH_400_002", "수정할 정보가 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_001", "아이디 또는 비밀번호가 일치하지 않습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_002", "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_003", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_004", "유효하지 않은 토큰입니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_401_005", "비밀번호가 일치하지 않습니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403_001", "해당 요청에 대한 권한이 없습니다."),
    WITHDRAWN_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH_403_002", "탈퇴한 계정입니다."),

    ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "AUTH_409_001", "이미 탈퇴 처리된 계정입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "AUTH_409_002", "이미 가입된 휴대폰 번호입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
