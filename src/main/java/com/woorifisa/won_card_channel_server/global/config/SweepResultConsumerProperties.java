package com.woorifisa.won_card_channel_server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sweep.result.consumer")
public record SweepResultConsumerProperties(
        boolean enabled,
        int fixedDelayMs,
        int maxMessages,
        int waitTimeSeconds,
        long processingTimeoutSeconds
) {
}
