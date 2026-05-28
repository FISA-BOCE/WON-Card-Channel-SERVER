package com.woorifisa.won_card_channel_server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sweep.outbox.publisher")
public record SweepOutboxPublisherProperties(
        boolean enabled,
        int batchSize,
        int maxRetryCount,
        long fixedDelayMs
) {
}
