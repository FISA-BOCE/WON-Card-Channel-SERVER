package com.woorifisa.won_card_channel_server.domain.ai.invest.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InvestSummary {

    private UUID userUuid;
    private UUID investAccountUuid;
    private long totalBuyAmount;
    private long profitLossAmount;
    private String etfSummaryJson;
}
