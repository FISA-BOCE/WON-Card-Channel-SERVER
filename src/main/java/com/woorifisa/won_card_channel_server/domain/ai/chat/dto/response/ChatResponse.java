package com.woorifisa.won_card_channel_server.domain.ai.chat.dto.response;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> contextUsed,
        List<String> suggestedQuestions
) {}
