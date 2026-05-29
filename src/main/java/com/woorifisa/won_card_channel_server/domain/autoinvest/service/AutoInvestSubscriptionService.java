package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public interface AutoInvestSubscriptionService {

    void createInitialSubscription(
            @NotNull UUID userUuid,
            @NotNull UUID invstAccountUuid,
            @NotNull Long etfId,
            @NotNull String ticker
    );
}
