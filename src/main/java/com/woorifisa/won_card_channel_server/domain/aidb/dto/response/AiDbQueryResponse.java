package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

public record AiDbQueryResponse<T>(
        AiDbQueryType queryType,
        T result
) {
}
