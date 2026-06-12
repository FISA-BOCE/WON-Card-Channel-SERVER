package com.woorifisa.won_card_channel_server.domain.card.external;

import com.woorifisa.won_card_channel_server.domain.card.dto.response.InvestAccountListResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "invest-channel-invest-account",
        url = "${internal.services.invest-channel.base-url}"
)
public interface InvestChannelInvestAccountApi {

    @GetMapping("/internal/invest/accounts")
    ApiResponse<InvestAccountListResponse> getInvestAccounts(
            @RequestHeader("X-User-UUID") UUID userUuid
    );
}
