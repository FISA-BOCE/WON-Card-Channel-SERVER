package com.woorifisa.won_card_channel_server.domain.sweep.dto.request;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.AutoSweepTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record InternalSweepRequestCreateRequest(
        @NotNull
        UUID userUuid,

        @NotNull
        UUID cardUserUuid,

        @NotNull
        UUID investUserUuid,

        @NotNull
        UUID investAccountUuid,

        @NotNull
        Long performanceId,

        @NotNull
        Long pointLedgerId,

        @NotBlank
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "baseMonth는 yyyy-MM 형식이어야 합니다.")
        String baseMonth,

        @NotNull
        @Positive
        Long pointAmount,

        @NotNull
        @Positive
        Long krwAmount,

        @NotNull
        Long etfId,

        @NotBlank
        String ticker
) {

    public AutoSweepTarget toTarget() {
        return new AutoSweepTarget(
                userUuid, cardUserUuid, investUserUuid, investAccountUuid,
                performanceId, pointLedgerId,
                baseMonth, pointAmount, krwAmount, etfId, ticker
        );
    }
}