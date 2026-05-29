package com.woorifisa.won_card_channel_server.domain.performance.dto.response;

public record PreviousPerformanceResponse(
        String baseMonth,
        String previousMonth,
        String rewardStatus,
        Long previousMonthSpendAmount,
        Detail detail
) {

    public record Detail(
            Long totalSpendAmount,
            Long rewardPointAmount
    ) {
    }
}
