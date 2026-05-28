package com.woorifisa.won_card_channel_server.domain.sweep.external.dto;

import java.util.List;
import java.util.UUID;

public record CardCoreSweepCandidateResponse(
        String baseMonth,
        List<CardCoreSweepCandidateItem> candidates
) {
    public record CardCoreSweepCandidateItem(
            Long pointLedgerId,
            UUID cardUserUuid,
            Long performanceId,
            String baseMonth,
            Long pointAmount,
            Long krwAmount
    ) {
    }
}
