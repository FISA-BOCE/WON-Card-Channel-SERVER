package com.woorifisa.won_card_channel_server.domain.auth.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.service.LedgerAuthClient;
import org.springframework.stereotype.Component;

@Component
public class StubLedgerAuthClient implements LedgerAuthClient {

    @Override
    public LedgerAuthResult fetchAuthenticationResult(String userId, String rawPassword) {
        // TODO(auth): 온프레미스 원장 대조 API 확정 후 실제 구현으로 교체
        return new LedgerAuthResult(true);
    }
}
