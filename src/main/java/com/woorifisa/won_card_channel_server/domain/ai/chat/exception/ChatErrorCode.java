package com.woorifisa.won_card_channel_server.domain.ai.chat.exception;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

    INVALID_QUESTION(HttpStatus.BAD_REQUEST, "AI_400_001", "질문 내용이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}