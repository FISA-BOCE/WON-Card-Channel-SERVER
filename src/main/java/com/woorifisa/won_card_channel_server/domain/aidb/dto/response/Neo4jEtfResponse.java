package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

public record Neo4jEtfResponse(
        Long etfId,
        String ticker,
        String etfName
) {
}
