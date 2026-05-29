package com.woorifisa.won_card_channel_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Bean
    @ConfigurationProperties(prefix = "azure.openai")
    public OpenAiProperties openAiProperties() {
        return new OpenAiProperties();
    }

    @Bean
    public WebClient openAiWebClient(OpenAiProperties openAiProperties) {
        return WebClient.builder()
                .baseUrl(openAiProperties.getEndpoint())
                .defaultHeader("api-key", openAiProperties.getApiKey())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Getter
    @Setter
    public static class OpenAiProperties {
        private String endpoint;
        private String apiKey;
        private String deploymentName;
        private String apiVersion;
    }

    @Bean
    public Duration openAiTimeout() {
        return Duration.ofSeconds(60);
    }
}
