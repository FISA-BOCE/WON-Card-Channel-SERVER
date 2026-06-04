package com.woorifisa.won_card_channel_server.domain.aidb.service;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jQueryResponse;

public interface Neo4jQueryService {

    Neo4jQueryResponse query(AiDbQueryRequest request);
}
