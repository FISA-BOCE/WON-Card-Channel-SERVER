package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

import java.util.List;
import java.util.UUID;

public record MyPointInvestmentPathResponse(
        Neo4jQueryType queryType,
        UUID userUuid,
        String baseMonth,
        List<InvestmentPath> investmentPaths
) {
}
