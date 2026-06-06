package com.woorifisa.won_card_channel_server.domain.chat.external;

import com.woorifisa.won_card_channel_server.domain.chat.dto.request.ChatRequest;
import com.woorifisa.won_card_channel_server.domain.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class CommonWasClient {

    private final WebClient commonWasWebClient;

    @Value("${internal.channel.service-id}")
    private String serviceId;

    @Value("${internal.channel.api-key}")
    private String internalApiKey;

    public ChatResponse sendChat(@NonNull String userUuid, String transactionId, ChatRequest request) {
        ApiResponse<ChatResponse> response = commonWasWebClient.post()
                .uri("/internal/chats")
                .header("X-Service-ID", serviceId)
                .header("X-Internal-Api-Key", internalApiKey)
                .header("X-User-UUID", userUuid)
                .headers(headers -> {
                    if (transactionId != null && !transactionId.isBlank()) {
                        headers.add("X-Transaction-ID", transactionId);
                    }
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<ChatResponse>>() {})
                .block();
        return response.data();
    }
}