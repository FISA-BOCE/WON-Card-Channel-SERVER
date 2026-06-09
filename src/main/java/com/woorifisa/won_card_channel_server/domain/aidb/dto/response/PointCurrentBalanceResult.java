package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

import java.math.BigDecimal;

public record PointCurrentBalanceResult(
        String baseMonth,
        BigDecimal currentPoint
) {
}
