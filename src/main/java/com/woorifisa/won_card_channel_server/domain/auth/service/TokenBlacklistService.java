package com.woorifisa.won_card_channel_server.domain.auth.service;

public interface TokenBlacklistService {

    void saveBlacklistedToken(String jti, long ttlMillis);

    boolean isBlacklisted(String jti);
}
