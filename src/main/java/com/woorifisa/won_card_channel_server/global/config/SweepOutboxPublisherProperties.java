package com.woorifisa.won_card_channel_server.global.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sweep.outbox.publisher")
public record SweepOutboxPublisherProperties(
        boolean enabled,

        @Positive
        int batchSize,

        @Positive
        int maxRetryCount,

        @Positive
        long fixedDelayMs,

        @Positive
        int messageGroupShardCount
) {
    public SweepOutboxPublisherProperties {
        if (messageGroupShardCount <= 0) {
            messageGroupShardCount = 64;
        }
    }
}
