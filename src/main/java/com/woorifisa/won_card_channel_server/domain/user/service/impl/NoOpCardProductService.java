package com.woorifisa.won_card_channel_server.domain.user.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.user.service.CardProductService;
import org.springframework.stereotype.Component;

@Component
public class NoOpCardProductService implements CardProductService {

    @Override
    public void terminateProducts(CardChnAuthUser user) {
        // TODO(auth): 카드 해지 처리 정책 확정 후 실제 구현으로 교체
    }
}
