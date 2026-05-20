package com.woorifisa.won_card_channel_server.domain.user.dto.response;

public record GetMyUserResponse(
        String userName,
        String tel,
        String createdAt
) {
}
