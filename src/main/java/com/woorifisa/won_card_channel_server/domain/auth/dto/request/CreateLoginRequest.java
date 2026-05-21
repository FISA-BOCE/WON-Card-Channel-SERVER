package com.woorifisa.won_card_channel_server.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLoginRequest(
        @NotBlank
        @Pattern(regexp = "^01[0-9][0-9]{7,8}$")
        String userId,
        @NotBlank
        @Size(min = 8)
        String userPw
) {
}
