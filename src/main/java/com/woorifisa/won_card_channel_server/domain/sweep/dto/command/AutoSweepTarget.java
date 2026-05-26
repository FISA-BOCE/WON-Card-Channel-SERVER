package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

import java.util.UUID;

public record AutoSweepTarget(
        UUID useruuid, UUID cardUserUuid, UUID investUuid, UUID investAccountUuid,
        Long performanceId, Long pointLedgerId, String baseMonth, Long pointAmount, Long krwAmount,
        Long etfId, String ticker
) {
}
