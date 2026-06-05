package com.woorifisa.won_card_channel_server.domain.aidb.dto.response;

public enum SweepExecutionStatus {
    RECEIVED,
    ACCEPTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRY_PENDING,
    CANCELLED
}
