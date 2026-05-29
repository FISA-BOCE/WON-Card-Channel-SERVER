package com.woorifisa.won_card_channel_server.domain.ai.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "질문을 입력해 주세요.")
        String question
) {}