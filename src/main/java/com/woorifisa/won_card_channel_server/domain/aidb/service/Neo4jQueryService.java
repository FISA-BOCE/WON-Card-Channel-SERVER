package com.woorifisa.won_card_channel_server.domain.aidb.service;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.Neo4jQueryRequest;

public interface Neo4jQueryService {

    Object query(Neo4jQueryRequest request);
}
