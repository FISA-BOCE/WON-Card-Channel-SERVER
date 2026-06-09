package com.woorifisa.won_card_channel_server.domain.card.external;

import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardCoreCardsResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "card-core-card", url = "${internal.services.card-core.base-url}")
public interface CardCoreCardApi {

    @GetMapping("/internal/cards")
    ApiResponse<CardCoreCardsResponse> getCards(
            @RequestHeader("X-User-UUID") UUID userUuid
    );
}
