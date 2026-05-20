package com.woorifisa.won_card_channel_server.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTokenReissueRequest(
        @NotBlank
        String refreshToken
) {
}
