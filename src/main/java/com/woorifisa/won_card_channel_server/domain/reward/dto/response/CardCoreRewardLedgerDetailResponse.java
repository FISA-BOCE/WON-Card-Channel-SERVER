package com.woorifisa.won_card_channel_server.domain.reward.dto.response;

import java.time.LocalDateTime;

public record CardCoreRewardLedgerDetailResponse(
        Long pointLedgerId,
        String baseMonth,
        String type,
        Long pointAmount,
        LocalDateTime occurredAt,
        Object detail
) {
}
