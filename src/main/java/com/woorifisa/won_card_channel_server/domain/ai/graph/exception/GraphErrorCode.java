package com.woorifisa.won_card_channel_server.domain.ai.graph.exception;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GraphErrorCode implements ErrorCode {

    UNSUPPORTED_QUERY_TYPE(HttpStatus.BAD_REQUEST, "GRAPH_400_001", "지원하지 않는 그래프 조회 유형입니다."),
    MERCHANT_REQUIRED(HttpStatus.BAD_REQUEST, "GRAPH_400_002", "가맹점명은 필수입니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "GRAPH_400_003", "유효하지 않은 기간 값입니다. (1MONTH / 3MONTH / 6MONTH / 1YEAR)"),
    GRAPH_DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GRAPH_500_001", "그래프 DB 조회에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GraphErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
