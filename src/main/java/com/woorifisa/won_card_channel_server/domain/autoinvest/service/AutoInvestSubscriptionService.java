package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.request.AutoInvestSubscriptionChangeRequest;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionChangeResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionDetailResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public interface AutoInvestSubscriptionService {

    void createInitialSubscription(
            @NotNull UUID userUuid,
            @NotNull UUID investAccountUuid,
            @NotNull @Positive Long etfId,
            @NotBlank String ticker
    );

    AutoInvestSubscriptionDetailResponse getSubscription(
            AuthenticatedUser authenticatedUser,
            @NotNull UUID cardUuid
    );

    AutoInvestSubscriptionChangeResponse changeSubscription(
            AuthenticatedUser authenticatedUser,
            @NotNull UUID cardUuid,
            @NotNull AutoInvestSubscriptionChangeRequest request
    );
}
