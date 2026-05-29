package com.woorifisa.won_card_channel_server.domain.card.external;

import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardCoreApplicationRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardCoreApplicationResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "card-core-application", contextId = "cardCoreCardApplicationApi", url = "${internal.card-core.url}")
public interface CardCoreCardApplicationApi {

    @PostMapping("/internal/cards/applications")
    ApiResponse<CardCoreApplicationResponse> applyCard(
            @RequestHeader("X-User-UUID") UUID userUuid,
            @RequestBody CardCoreApplicationRequest request
    );
}
