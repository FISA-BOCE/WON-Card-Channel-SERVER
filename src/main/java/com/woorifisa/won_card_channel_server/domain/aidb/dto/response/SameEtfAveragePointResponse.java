package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SameEtfAveragePointResponse(
        Neo4jQueryType queryType,
        UUID userUuid,
        Neo4jEtfResponse selectedEtf,
        Long sameEtfUserCount,
        BigDecimal averagePointAmount,
        BigDecimal totalPointAmount,
        String baseMonth
) {
}
