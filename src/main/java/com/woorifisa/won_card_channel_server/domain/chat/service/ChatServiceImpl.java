package com.woorifisa.won_card_channel_server.domain.chat.service;

import com.woorifisa.won_card_channel_server.domain.chat.dto.request.ChatRequest;
import com.woorifisa.won_card_channel_server.domain.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.domain.chat.external.CommonWasClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final CommonWasClient commonWasClient;

    @Override
    public ChatResponse processChat(UUID userUuid, String transactionId, ChatRequest request) {
        return commonWasClient.sendChat(userUuid, transactionId, request);
    }
}