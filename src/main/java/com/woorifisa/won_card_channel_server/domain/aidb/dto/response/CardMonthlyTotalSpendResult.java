package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

import java.math.BigDecimal;

public record CardMonthlyTotalSpendResult(
        String baseMonth,
        BigDecimal totalSpendAmount,
        BigDecimal foodAmount,
        BigDecimal shoppingAmount,
        BigDecimal transportAmount,
        BigDecimal subscriptionAmount,
        BigDecimal etcAmount
) {
}
