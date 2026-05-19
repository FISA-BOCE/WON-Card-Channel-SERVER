package com.woorifisa.won_card_channel_server.global.exception.handler;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import com.woorifisa.won_card_channel_server.global.util.ValidationUtils;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(resolveMessage(errorCode, message));
        this.errorCode = errorCode;
        this.message = resolveMessage(errorCode, message);
    }

    @Override
    public String getMessage() {
        return message;
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        return ValidationUtils.isBlank(message) ? errorCode.getMessage() : message;
    }
}
