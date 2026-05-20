package com.woorifisa.won_card_channel_server.domain.auth.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnTokenBlacklist;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnTokenBlacklistRepository;
import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final CardChnTokenBlacklistRepository tokenBlacklistRepository;

    @Override
    public void saveBlacklistedToken(String jti, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }

        tokenBlacklistRepository.save(CardChnTokenBlacklist.builder()
                .accessTokenJti(jti)
                .expiredAt(LocalDateTime.now().plusNanos(ttlMillis * 1_000_000))
                .build());
    }

    @Override
    public boolean isBlacklisted(String jti) {
        tokenBlacklistRepository.deleteByExpiredAtBefore(LocalDateTime.now());
        if (jti == null) {
            return false;
        }
        return tokenBlacklistRepository.existsById(jti);
    }
}
