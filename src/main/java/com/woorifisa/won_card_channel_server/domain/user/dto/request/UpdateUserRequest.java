package com.woorifisa.won_card_channel_server.domain.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email
        @Size(max = 255)
        String email,
        @Size(min = 8)
        String currentPw,
        @Size(min = 8)
        String newPw
) {

    @AssertTrue
    public boolean hasAtLeastOneUpdateTarget() {
        return !isBlank(email) || !isBlank(currentPw) || !isBlank(newPw);
    }

    @AssertTrue
    public boolean hasValidPasswordUpdatePair() {
        boolean hasCurrentPw = !isBlank(currentPw);
        boolean hasNewPw = !isBlank(newPw);
        return hasCurrentPw == hasNewPw;
    }

    @AssertTrue
    public boolean hasDifferentNewPassword() {
        if (isBlank(currentPw) || isBlank(newPw)) {
            return true;
        }
        return !currentPw.equals(newPw);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
