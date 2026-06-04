package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvestmentPath(
        Long sweepRequestId,
        BigDecimal pointAmount,
        BigDecimal krwAmount,
        SweepRequestStatus requestStatus,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        Neo4jEtfResponse targetEtf,
        SweepExecutionResponse execution
) {
}
