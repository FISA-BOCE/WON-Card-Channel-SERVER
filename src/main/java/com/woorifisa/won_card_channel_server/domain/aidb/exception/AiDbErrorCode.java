package com.woorifisa.won_card_channel_server.domain.aidb.exception;

import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiDbErrorCode implements ErrorCode {

    UNSUPPORTED_QUERY_TYPE(HttpStatus.BAD_REQUEST, "CHAT_400_003", "지원하지 않는 queryType입니다."),
    INVALID_QUERY_PARAM(HttpStatus.BAD_REQUEST, "CHAT_400_004", "요청 파라미터가 올바르지 않습니다."),
    QUERY_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_001", "조회 결과가 없습니다."),
    MYSQL_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT_500_001", "MySQL 조회 중 오류가 발생했습니다."),
    GRAPH_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT_500_002", "Neo4j 조회 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}