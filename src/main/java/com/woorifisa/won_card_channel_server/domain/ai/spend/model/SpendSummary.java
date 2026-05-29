package com.woorifisa.won_card_channel_server.domain.ai.spend.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SpendSummary {

    private UUID userUuid;
    private String baseMonth;
    private long totalSpendAmount;
    private long foodAmount;
    private long shoppingAmount;
    private long transportAmount;
    private long subscriptionAmount;
    private long etcAmount;
    private String topMerchantJson;
    private long pointBalanceAmount;
    private long currentMonthSpendAmount;
    private double rewardRate;
    private String performanceStatus;
}