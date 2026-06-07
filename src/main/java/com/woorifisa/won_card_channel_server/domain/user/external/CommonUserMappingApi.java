package com.woorifisa.won_card_channel_server.domain.user.external;

import com.woorifisa.won_card_channel_server.domain.user.dto.request.InitializeUserMappingRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateCardUserMappingRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserMappingResponse;
import com.woorifisa.won_card_channel_server.domain.user.external.config.CommonUserMappingFeignConfig;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "common-user-mapping",
        url = "${internal.services.common.base-url}",
        configuration = CommonUserMappingFeignConfig.class
)
public interface CommonUserMappingApi {

    @PostMapping("/internal/mappings/users")
    ApiResponse<GetMyUserMappingResponse> initializeUserMapping(
            @RequestBody InitializeUserMappingRequest request
    );

    @GetMapping("/internal/mappings/users/{userUuid}")
    ApiResponse<GetMyUserMappingResponse> getMappingStatus(
            @PathVariable("userUuid") UUID userUuid
    );

    @PatchMapping("/internal/mappings/users/{userUuid}/card")
    ApiResponse<GetMyUserMappingResponse> updateCardUserMapping(
            @PathVariable("userUuid") UUID userUuid,
            @RequestBody UpdateCardUserMappingRequest request
    );
}
