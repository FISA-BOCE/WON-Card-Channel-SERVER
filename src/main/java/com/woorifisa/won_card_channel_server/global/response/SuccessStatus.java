package com.woorifisa.won_card_channel_server.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessStatus {

    OK(HttpStatus.OK, "OK"),
    CREATED(HttpStatus.CREATED, "CREATED"),
    NO_CONTENT(HttpStatus.NO_CONTENT, "NO_CONTENT");

    private final HttpStatus httpStatus;
    private final String message;

    SuccessStatus(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

}