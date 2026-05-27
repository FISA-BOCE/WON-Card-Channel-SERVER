package com.woorifisa.won_card_channel_server.domain.sweep.dto.response;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepRequest;

public record SweepRequestCreateResponse(
        Long sweepRequestId, String requestStatus,
        Long pointLedgerId, Long krwAmount, String ticker
) {
    public static SweepRequestCreateResponse from(CardChnSweepRequest sweepRequest) {
        return new SweepRequestCreateResponse(
                sweepRequest.getSweepRequestId(),
                sweepRequest.getRequestStatus().name(),
                sweepRequest.getPointLedgerId(),
                sweepRequest.getKrwAmount(),
                sweepRequest.getTicker()
        );
    }
}
