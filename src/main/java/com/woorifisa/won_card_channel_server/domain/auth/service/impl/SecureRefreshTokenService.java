package com.woorifisa.won_card_channel_server.domain.auth.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.service.RefreshTokenService;
import com.woorifisa.won_card_channel_server.global.config.SecurityProperties;
import com.woorifisa.won_card_channel_server.global.util.HashUtils;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecureRefreshTokenService implements RefreshTokenService {

    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String createRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String createTokenHash(String rawToken) {
        return HashUtils.sha256(rawToken);
    }

    @Override
    public LocalDateTime getRefreshTokenExpiryAt() {
        return LocalDateTime.now().plusSeconds(securityProperties.getRefreshTokenExpirationSeconds());
    }
}
