package com.woorifisa.won_card_channel_server.domain.user.external.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class CommonUserMappingFeignConfig {

    private static final long CONNECT_TIMEOUT_MILLIS = 3_000L;
    private static final long READ_TIMEOUT_MILLIS = 5_000L;

    @Bean
    public Request.Options commonUserMappingRequestOptions() {
        return new Request.Options(
                CONNECT_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                READ_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    public Retryer commonUserMappingRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public RequestInterceptor internalAuthInterceptor(
            @Value("${internal.channel.service-id}") String serviceId,
            @Value("${internal.channel.api-key}") String apiKey
    ) {
        return template -> {
            template.header("X-Service-ID", serviceId);
            template.header("X-Internal-Api-Key", apiKey);
        };
    }
}
