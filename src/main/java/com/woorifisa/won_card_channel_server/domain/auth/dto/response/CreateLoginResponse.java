package com.woorifisa.won_card_channel_server.domain.auth.dto.response;

public record CreateLoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
