package com.woorifisa.won_card_channel_server.domain.auth.dto;

public record CreateTokenReissueResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
