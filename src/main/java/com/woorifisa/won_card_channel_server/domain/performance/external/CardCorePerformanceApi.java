package com.woorifisa.won_card_channel_server.domain.performance.external;

import com.woorifisa.won_card_channel_server.domain.performance.dto.response.PreviousPerformanceResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "card-core-card", contextId = "card-core-performance", url = "${internal.card-core.url}")
public interface CardCorePerformanceApi {

    @GetMapping("/internal/cards/performance/monthly")
    ApiResponse<PreviousPerformanceResponse> getMonthlyPerformance(
            @RequestHeader("X-User-UUID") UUID userUuid
    );
}
