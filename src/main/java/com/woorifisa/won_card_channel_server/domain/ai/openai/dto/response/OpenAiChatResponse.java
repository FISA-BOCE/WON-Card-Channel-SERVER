package com.woorifisa.won_card_channel_server.domain.ai.openai.dto.response;

import java.util.List;

public record OpenAiChatResponse(
        List<Choice> choices
) {
    public record Choice(Message message) {}

    public record Message(String role, String content) {}

    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        Choice first = choices.get(0);
        if (first == null || first.message() == null || first.message().content() == null) {
            return "";
        }
        return first.message().content();
    }
}