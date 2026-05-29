package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public interface AutoInvestSubscriptionService {

    void createInitialSubscription(
            @NotNull UUID userUuid,
            @NotNull UUID invstAccountUuid,
            @NotNull @Positive Long etfId,
            @NotBlank String ticker
    );
}
