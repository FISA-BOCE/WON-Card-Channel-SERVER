package com.woorifisa.won_card_channel_server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.sqs")
public record SqsProperties(
        String region,
        String endpoint,
        String sweepRequestQueueUrl
) {
}
