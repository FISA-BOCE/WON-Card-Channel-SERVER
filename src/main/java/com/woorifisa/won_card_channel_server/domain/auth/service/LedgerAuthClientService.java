package com.woorifisa.won_card_channel_server.domain.auth.service;

public interface LedgerAuthClientService {

    LedgerAuthResult fetchAuthenticationResult(String userId, String rawPassword);

    record LedgerAuthResult(boolean authenticated) {
    }
}
