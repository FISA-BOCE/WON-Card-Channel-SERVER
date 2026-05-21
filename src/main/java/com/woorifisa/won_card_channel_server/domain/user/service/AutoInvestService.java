package com.woorifisa.won_card_channel_server.domain.user.service;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;

public interface AutoInvestService {

    void disableAutoInvest(CardChnAuthUser user);
}
