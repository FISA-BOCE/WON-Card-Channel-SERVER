package com.woorifisa.won_card_channel_server.domain.chat.exception.code;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorCode implements ErrorCode {

    CHAT_EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "CHAT_400_001", "질문 내용이 없습니다."),
    COMMON_WAS_ERROR(HttpStatus.BAD_GATEWAY, "CHAT_502_001", "AI 서버 응답 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ChatErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}