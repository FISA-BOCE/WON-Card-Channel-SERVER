package com.woorifisa.won_card_channel_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private String jwtSecret;
    private String cryptoSecret;
    private long accessTokenExpirationSeconds;
    private long refreshTokenExpirationSeconds;
}