package com.woorifisa.won_card_channel_server.domain.auth.service;

public interface LedgerAuthClient {

    LedgerAuthResult fetchAuthenticationResult(String userId, String rawPassword);

    record LedgerAuthResult(boolean authenticated) {
    }
}
