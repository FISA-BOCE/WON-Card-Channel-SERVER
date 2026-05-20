package com.woorifisa.won_card_channel_server.global.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @NotBlank
    private String jwtSecret;

    @NotBlank
    private String cryptoSecret;

    @Positive
    private long accessTokenExpirationSeconds;

    @Positive
    private long refreshTokenExpirationSeconds;
}
