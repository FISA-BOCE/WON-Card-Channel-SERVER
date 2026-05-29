package com.woorifisa.won_card_channel_server.domain.autoinvest.external;

import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestEtfDetailsResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "invest-channel-auto-invest", url = "${internal.invest-core.url}")
public interface InvestChannelAutoInvestApi {

    @GetMapping("/internal/invest/accounts/{investAccountUuid}")
    ApiResponse<InvestAccountDetailsResponse> getInvestmentAccount(
            @RequestHeader("X-User-UUID") UUID userUuid,
            @PathVariable("investAccountUuid") UUID investAccountUuid
    );

    @GetMapping("/internal/invest/etfs/{etfId}")
    ApiResponse<InvestEtfDetailsResponse> getEtf(
            @PathVariable("etfId") Long etfId
    );
}
