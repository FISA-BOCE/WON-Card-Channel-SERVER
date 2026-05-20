package com.woorifisa.won_card_channel_server.global.security;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@RequiredArgsConstructor
public class RequestAccessTokenHolder {

    public static final String ACCESS_TOKEN_ATTRIBUTE = "requestAccessToken";

    private final HttpServletRequest request;

    public String getRequiredToken() {
        Object token = request.getAttribute(ACCESS_TOKEN_ATTRIBUTE);
        if (!(token instanceof String value) || value.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }
        return value;
    }
}
