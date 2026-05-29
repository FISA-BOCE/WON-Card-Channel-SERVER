package com.woorifisa.won_card_channel_server.global.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
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
    @Validated
    public static class OpenAiProperties {
        @NotBlank private String endpoint;
        @NotBlank private String apiKey;
        @NotBlank private String deploymentName;
        @NotBlank private String apiVersion;
    }

    @Bean
    public Duration openAiTimeout() {
        return Duration.ofSeconds(60);
    }
}
