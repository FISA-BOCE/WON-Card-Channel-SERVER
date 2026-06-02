package com.woorifisa.won_card_channel_server.domain.ai.openai.exception;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OpenAiErrorCode implements ErrorCode {

    OPENAI_API_ERROR(HttpStatus.BAD_GATEWAY, "AI_502_001", "AI 서버와 통신 중 오류가 발생했습니다."),
    OPENAI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI_504_001", "AI 서버 응답 시간이 초과되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
