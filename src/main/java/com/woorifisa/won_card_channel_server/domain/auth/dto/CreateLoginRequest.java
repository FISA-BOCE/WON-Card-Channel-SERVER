package com.woorifisa.won_card_channel_server.domain.auth.dto;

public record CreateLoginRequest(
        String userId,
        String userPw
) {
}
