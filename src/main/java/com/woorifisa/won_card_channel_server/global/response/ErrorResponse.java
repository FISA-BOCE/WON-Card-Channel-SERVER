package com.woorifisa.won_card_channel_server.global.response;


import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;

public record ErrorResponse(
        int status,
        String code,
        String message
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                message
        );
    }
}