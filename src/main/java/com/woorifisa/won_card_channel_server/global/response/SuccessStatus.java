package com.woorifisa.won_card_channel_server.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessStatus {

    OK(HttpStatus.OK, "SUCCESS", "OK"),
    CREATED(HttpStatus.CREATED, "SUCCESS", "CREATED"),
    NO_CONTENT(HttpStatus.NO_CONTENT, "SUCCESS", "NO_CONTENT"),

    // 인증 및 회원
    SIGNUP_SUCCESS(HttpStatus.OK, "SUCCESS", "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS(HttpStatus.OK, "SUCCESS", "로그인이 완료되었습니다."),
    TOKEN_REISSUE_SUCCESS(HttpStatus.OK, "SUCCESS", "토큰 재발급이 완료되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "SUCCESS", "로그아웃이 완료되었습니다."),
    USER_ME_SUCCESS(HttpStatus.OK, "SUCCESS", "사용자 정보 조회가 완료되었습니다."),
    USER_WITHDRAW_SUCCESS(HttpStatus.OK, "SUCCESS", "회원 탈퇴가 완료되었습니다."),
    USER_UPDATE_SUCCESS(HttpStatus.OK, "SUCCESS", "회원 정보 수정이 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SuccessStatus(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}
