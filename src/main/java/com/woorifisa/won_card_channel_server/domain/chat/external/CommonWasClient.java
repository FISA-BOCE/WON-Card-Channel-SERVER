package com.woorifisa.won_card_channel_server.domain.chat.external;

import com.woorifisa.won_card_channel_server.domain.chat.dto.request.ChatRequest;
import com.woorifisa.won_card_channel_server.domain.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.domain.chat.exception.code.ChatErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class CommonWasClient {

    private final WebClient commonWasWebClient;

    @Value("${internal.service-id}")
    private String serviceId;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public ChatResponse sendChat(@NonNull UUID userUuid, String transactionId, ChatRequest request) {
        ApiResponse<ChatResponse> response = commonWasWebClient.post()
                .uri("/internal/chats")
                .header("X-Service-ID", serviceId)
                .header("X-Internal-Api-Key", internalApiKey)
                .header("X-User-UUID", userUuid.toString())
                .headers(headers -> {
                    if (transactionId != null && !transactionId.isBlank()) {
                        headers.add("X-Transaction-ID", transactionId);
                    }
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<ChatResponse>>() {})
                .block();
        if (response == null || response.data() == null) {
            throw new BusinessException(ChatErrorCode.COMMON_WAS_ERROR);
        }
        return response.data();
    }
}
