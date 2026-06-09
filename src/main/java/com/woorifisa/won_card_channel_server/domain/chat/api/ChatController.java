package com.woorifisa.won_card_channel_server.domain.chat.api;

import com.woorifisa.won_card_channel_server.domain.chat.dto.request.ChatRequest;
import com.woorifisa.won_card_channel_server.domain.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.domain.chat.service.ChatService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
@Tag(name = "Chat", description = "AI 챗봇 API")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "AI 챗봇 질문", description = "사용자 자연어 질문을 수신하여 AI 답변을 반환합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "트랜잭션 추적용 ID")
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId,
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatService.processChat(authenticatedUser.userUuid(), transactionId, request);
        return ResponseEntity
                .status(SuccessStatus.CHAT_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.CHAT_SUCCESS, response));
    }
}
