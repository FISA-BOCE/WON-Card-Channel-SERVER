package com.woorifisa.won_card_channel_server.domain.sweep.dto.response;

public record CardCoreSweepRequestResponse(
        Long pointLedgerId,
        Long performanceId,
        String baseMonth,
        Long pointAmount,
        Long krwAmount,
        String sweepStatus
) {
}
