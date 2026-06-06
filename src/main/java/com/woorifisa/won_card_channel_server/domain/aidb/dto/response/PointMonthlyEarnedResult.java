package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

import java.math.BigDecimal;

public record PointMonthlyEarnedResult(
        String baseMonth,
        BigDecimal currentMonthEarnedAmount
) {
}
