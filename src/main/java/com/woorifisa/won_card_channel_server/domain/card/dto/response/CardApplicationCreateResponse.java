package com.woorifisa.won_card_channel_server.domain.card.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

// 카드 신청 완료 화면 기준
public record CardApplicationCreateResponse(
        UUID cardUuid,
        String cardNoDisplay,
        LocalDateTime issuedAt,
        String cardStatus,
        String autoInvestEtfName
) {
}
