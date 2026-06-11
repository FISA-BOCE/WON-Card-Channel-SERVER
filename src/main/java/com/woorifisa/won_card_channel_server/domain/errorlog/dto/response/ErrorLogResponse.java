package com.woorifisa.won_card_channel_server.domain.errorlog.dto.response;

import com.woorifisa.won_card_channel_server.global.logging.ErrorLog;

import java.time.LocalDateTime;

public record ErrorLogResponse(
        Long id,
        int status,
        String method,
        String uri,
        long elapsedMs,
        LocalDateTime createdAt
) {
    public static ErrorLogResponse from(ErrorLog errorLog) {
        return new ErrorLogResponse(
                errorLog.getId(),
                errorLog.getStatus(),
                errorLog.getMethod(),
                errorLog.getUri(),
                errorLog.getElapsedMs(),
                errorLog.getCreatedAt()
        );
    }
}
