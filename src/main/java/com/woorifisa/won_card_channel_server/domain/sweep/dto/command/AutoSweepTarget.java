package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.request.InternalSweepRequestCreateRequest;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.response.CardCoreSweepRequestResponse;

import java.util.UUID;

public record AutoSweepTarget(
        UUID userUuid, UUID cardUserUuid, UUID investUserUuid, UUID investAccountUuid,
        Long performanceId, Long pointLedgerId, String baseMonth, Long pointAmount, Long krwAmount,
        Long etfId, String ticker
) {
    public static AutoSweepTarget of(InternalSweepRequestCreateRequest request, CardCoreSweepRequestResponse coreResponse) {
        return new AutoSweepTarget(
                request.userUuid(),
                request.cardUserUuid(),
                request.investUserUuid(),
                request.investAccountUuid(),
                coreResponse.performanceId(),
                coreResponse.pointLedgerId(),
                coreResponse.baseMonth(),
                coreResponse.pointAmount(),
                coreResponse.krwAmount(),
                request.etfId(),
                request.ticker()
        );
    }
}
