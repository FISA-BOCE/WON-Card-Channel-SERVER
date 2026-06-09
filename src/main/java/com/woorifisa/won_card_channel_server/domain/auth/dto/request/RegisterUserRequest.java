package com.woorifisa.won_card_channel_server.domain.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank
        @Pattern(regexp = "^01[0-9][0-9]{7,8}$")
        String phoneNumber,
        @NotBlank
        @Size(max = 100)
        String userName,
        @NotBlank
        @Size(min = 8)
        String password,
        @NotBlank
        @Size(min = 8)
        String passwordConfirm,
        @NotBlank
        @Email
        @Size(max = 255)
        String email,
        @NotNull
        Boolean termsAgreed
) {

    @AssertTrue
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(passwordConfirm);
    }

    @AssertTrue
    public boolean isTermsAgreedAccepted() {
        return Boolean.TRUE.equals(termsAgreed);
    }
}
