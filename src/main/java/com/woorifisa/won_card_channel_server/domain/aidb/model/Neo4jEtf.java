package com.woorifisa.won_card_channel_server.domain.aidb.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Neo4jEtf {

    public static final String LABEL = "ETF";

    private Long etfId;
    private String ticker;
    private String etfName;
}
