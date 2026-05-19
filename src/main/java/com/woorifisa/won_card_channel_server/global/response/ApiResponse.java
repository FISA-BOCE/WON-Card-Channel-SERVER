package com.woorifisa.won_card_channel_server.global.response;

public record ApiResponse<T>(
        int status,
        String code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> of(SuccessStatus successStatus, T data) {
        return new ApiResponse<>(
                successStatus.getHttpStatus().value(),
                successStatus.getCode(),
                successStatus.getMessage(),
                data
        );
    }

    public static ApiResponse<Void> of(SuccessStatus successStatus) {
        return new ApiResponse<>(
                successStatus.getHttpStatus().value(),
                successStatus.getCode(),
                successStatus.getMessage(),
                null
        );
    }
}