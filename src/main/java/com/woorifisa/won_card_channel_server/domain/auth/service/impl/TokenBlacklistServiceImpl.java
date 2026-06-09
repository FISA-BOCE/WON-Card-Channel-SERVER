package com.woorifisa.won_card_channel_server.domain.auth.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnTokenBlacklist;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnTokenBlacklistRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final CardChnTokenBlacklistRepository tokenBlacklistRepository;

    @Override
    public void saveBlacklistedToken(String jti, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }

        tokenBlacklistRepository.save(CardChnTokenBlacklist.builder()
                .accessTokenJti(jti)
                .expiredAt(currentKstDateTime().plus(Duration.ofMillis(ttlMillis)))
                .build());
    }

    @Override
    public boolean isBlacklisted(String jti) {
        tokenBlacklistRepository.deleteByExpiredAtBefore(currentKstDateTime());
        if (jti == null) {
            return false;
        }
        return tokenBlacklistRepository.existsById(jti);
    }

    private LocalDateTime currentKstDateTime() {
        return LocalDateTime.now(KST_ZONE_ID);
    }
}
