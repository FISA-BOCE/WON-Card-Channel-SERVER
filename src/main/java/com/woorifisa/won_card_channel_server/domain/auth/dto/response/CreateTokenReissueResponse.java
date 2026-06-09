package com.woorifisa.won_card_channel_server.domain.auth.dto.response;

public record CreateTokenReissueResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
