package com.woorifisa.won_card_channel_server.domain.user.external;

import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateCardUserMappingRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserMappingResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "common-user-mapping", url = "${internal.common.url:http://localhost:8080}")
public interface CommonUserMappingApi {

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
