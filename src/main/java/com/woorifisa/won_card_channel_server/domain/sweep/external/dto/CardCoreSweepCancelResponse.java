package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

public record CardCoreSweepCancelResponse(
        Long pointLedgerId,
        String sweepStatus
) {
}
