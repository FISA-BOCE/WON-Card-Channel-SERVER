package com.woorifisa.won_card_channel_server.domain.auth.dto;

public record RegisterUserRequest(
        String phoneNumber,
        String userName,
        String password,
        String passwordConfirm,
        String email,
        Boolean termsAgreed
) {
}
