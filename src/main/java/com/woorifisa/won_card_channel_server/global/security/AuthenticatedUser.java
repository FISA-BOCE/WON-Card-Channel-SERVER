package com.woorifisa.won_card_channel_server.global.security;

public record AuthenticatedUser(
        String authUserUuid,
        String userUuid,
        String jti,
        String token
) {
}
