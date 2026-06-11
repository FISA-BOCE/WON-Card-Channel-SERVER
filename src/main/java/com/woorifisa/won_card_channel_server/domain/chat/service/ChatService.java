package com.woorifisa.won_card_channel_server.domain.chat.service;

import com.woorifisa.won_card_channel_server.domain.chat.dto.request.ChatRequest;
import com.woorifisa.won_card_channel_server.domain.chat.dto.response.ChatResponse;

import java.util.UUID;

public interface ChatService {
    ChatResponse processChat(UUID userUuid, String transactionId, ChatRequest request);
}