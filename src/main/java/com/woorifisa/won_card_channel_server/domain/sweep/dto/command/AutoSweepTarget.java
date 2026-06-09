package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

import com.woorifisa.won_card_channel_server.domain.sweep.external.dto.CardCoreSweepRequestResponse;

import java.util.UUID;

public record AutoSweepTarget(
        UUID userUuid, UUID cardUserUuid,
        Long performanceId, Long pointLedgerId, String baseMonth, Long pointAmount, Long krwAmount,
        Long etfId
) {
    public static AutoSweepTarget of(AutoSweepCreateCommand request, CardCoreSweepRequestResponse coreResponse) {
        return new AutoSweepTarget(
                request.userUuid(),
                request.cardUserUuid(),
                coreResponse.performanceId(),
                coreResponse.pointLedgerId(),
                coreResponse.baseMonth(),
                coreResponse.pointAmount(),
                coreResponse.krwAmount(),
                request.etfId()
        );
    }

    public static AutoSweepTarget of(ReservedSweepCreateCommand command) {
        return new AutoSweepTarget(
                command.userUuid(),
                command.cardUserUuid(),
                command.performanceId(),
                command.pointLedgerId(),
                command.baseMonth(),
                command.pointAmount(),
                command.krwAmount(),
                command.etfId()
        );
    }
}
