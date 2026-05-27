package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record NoCardSummaryResponse(
        boolean hasCard,
        CardProduct cardProduct
) {

    public NoCardSummaryResponse(CardProduct cardProduct) {
        this(false, cardProduct);
    }

    public record CardProduct(
            String productName,
            BigDecimal rewardRateMin,
            BigDecimal rewardRateMax,
            Long monthlyLimitAmount,
            List<String> benefits
    ) {
    }
}
