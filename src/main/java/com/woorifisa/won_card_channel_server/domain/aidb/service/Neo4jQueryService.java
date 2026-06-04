package com.woorifisa.won_card_channel_server.domain.aidb.service;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;

public interface Neo4jQueryService {

    Object query(AiDbQueryRequest request);
}
