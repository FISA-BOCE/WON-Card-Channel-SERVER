package com.woorifisa.won_card_channel_server.global.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sweep.batch")
public record SweepBatchProperties(
        @Positive
        int reservationSize
) {
    public SweepBatchProperties {
        if (reservationSize <= 0) {
            reservationSize = 500;
        }
    }
}
