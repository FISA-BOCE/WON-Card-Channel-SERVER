package com.woorifisa.won_card_channel_server.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.sqs")
public record SqsProperties(
        @NotBlank
        String region,

        String endpoint,

        @NotBlank
        String sweepRequestQueueUrl,

        @NotBlank
        String sweepResultQueueUrl
) {
}
