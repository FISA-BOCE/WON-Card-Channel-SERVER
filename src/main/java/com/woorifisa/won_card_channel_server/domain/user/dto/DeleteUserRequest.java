package com.woorifisa.won_card_channel_server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteUserRequest(
        @NotBlank
        String refreshToken
) {
}
