package com.woorifisa.won_card_channel_server.domain.aidb.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Neo4jSweepExecution {

    public static final String LABEL = "SweepExecution";

    private Long sweepId;
    private Long sweepRequestId;
    private Long krwAmount;
    private String sweepStatus;
    private String correlationId;
    private String idempotencyKey;
    private LocalDateTime receivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String failReason;
}
