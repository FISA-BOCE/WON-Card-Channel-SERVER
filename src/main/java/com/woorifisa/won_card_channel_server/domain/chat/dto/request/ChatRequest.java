package com.woorifisa.won_card_channel_server.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {}
