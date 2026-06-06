package com.woorifisa.won_card_channel_server.domain.aidb.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Neo4jSweepRequest {

    public static final String LABEL = "SweepRequest";

    private Long sweepRequestId;
    private Long performanceId;
    private Long pointLedgerId;
    private String baseMonth;
    private Long pointAmount;
    private Long krwAmount;
    private String requestStatus;
    private String correlationId;
    private String idempotencyKey;
    private LocalDateTime requestedAt;
    private LocalDateTime sentAt;
    private LocalDateTime completedAt;
}
