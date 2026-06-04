package com.woorifisa.won_card_channel_server.global.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sweep.result.consumer")
public record SweepResultConsumerProperties(
        boolean enabled,
        @Min(1) @Max(20) int maxMessages,
        @Min(0) @Max(20) int waitTimeSeconds,
        @Min(3) long processingTimeoutSeconds,
        @Min(1) @Max(64) int workerCount
) {
}
