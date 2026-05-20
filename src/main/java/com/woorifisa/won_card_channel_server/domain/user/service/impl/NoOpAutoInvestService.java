package com.woorifisa.won_card_channel_server.domain.user.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.user.service.AutoInvestService;
import org.springframework.stereotype.Component;

@Component
public class NoOpAutoInvestService implements AutoInvestService {

    @Override
    public void disableAutoInvest(CardChnAuthUser user) {
        // TODO(auth): 자동투자 해지 정책 확정 후 실제 구현으로 교체
    }
}