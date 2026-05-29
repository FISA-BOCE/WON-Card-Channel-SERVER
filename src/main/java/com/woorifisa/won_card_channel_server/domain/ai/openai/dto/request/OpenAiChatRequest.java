package com.woorifisa.won_card_channel_server.domain.ai.openai.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatRequest(
        List<Message> messages,
        double temperature,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {
    public record Message(String role, String content) {
        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }
    }

    public record ResponseFormat(String type) {
        public static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object");
        }
    }
}