package com.woorifisa.won_card_channel_server.domain.sweep.dto.response;

import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;

public record SweepRequestCreateResponse(
        Long sweepRequestId, String requestStatus,
        Long pointLedgerId, Long krwAmount, Long etfId
) {
    public static SweepRequestCreateResponse from(Sweep sweep) {
        return new SweepRequestCreateResponse(
                sweep.getSweepRequestId(),
                sweep.getRequestStatus().name(),
                sweep.getPointLedgerId(),
                sweep.getKrwAmount(),
                sweep.getEtfId()
        );
    }
}
