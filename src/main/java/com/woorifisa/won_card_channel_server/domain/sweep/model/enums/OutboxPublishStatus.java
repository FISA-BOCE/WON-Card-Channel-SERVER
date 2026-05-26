package com.woorifisa.won_card_channel_server.domain.sweep.model.enums;

public enum OutboxPublishStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    RETRY,
    FAILED
}
