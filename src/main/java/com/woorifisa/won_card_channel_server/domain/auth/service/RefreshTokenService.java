package com.woorifisa.won_card_channel_server.domain.auth.service;

import java.time.LocalDateTime;

public interface RefreshTokenService {

    String createRefreshToken();

    String createTokenHash(String rawToken);

    LocalDateTime getRefreshTokenExpiryAt();
}
