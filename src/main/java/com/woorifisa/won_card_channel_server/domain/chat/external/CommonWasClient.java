package com.woorifisa.won_card_channel_server.domain.chat.external;

import com.woorifisa.won_card_channel_server.domain.chat.dto.request.ChatRequest;
import com.woorifisa.won_card_channel_server.domain.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class CommonWasClient {

    private final WebClient commonWasWebClient;

    public ChatResponse sendChat(String userUuid, String transactionId, ChatRequest request) {
        ApiResponse<ChatResponse> response = commonWasWebClient.post()
                .uri("/internal/chats")
                .header("X-User-UUID", userUuid)
                .headers(headers -> {
                    if (transactionId != null) {
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