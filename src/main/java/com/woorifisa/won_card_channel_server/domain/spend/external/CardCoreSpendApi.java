package com.woorifisa.won_card_channel_server.domain.spend.external;

import com.woorifisa.won_card_channel_server.domain.spend.dto.response.SpendCurrentAmountResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "card-core-card", contextId = "card-core-spend", url = "${internal.card-core.url}")
public interface CardCoreSpendApi {

    @GetMapping("/internal/cards/spend-summary")
    ApiResponse<SpendCurrentAmountResponse> getSpendSummary(
            @RequestHeader("X-User-UUID") UUID userUuid
    );
}
