package com.woorifisa.won_card_channel_server.domain.ai.chat.api;

import com.woorifisa.won_card_channel_server.domain.ai.chat.dto.request.ChatRequest;
import com.woorifisa.won_card_channel_server.domain.ai.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.domain.ai.chat.service.ChatService;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "AI Chat", description = "개인화 챗봇 API")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatApi {

    private final ChatService chatService;

    @Operation(summary = "개인화 챗봇 질문", description = "사용자 질문을 받아 SQL/GraphDB 조회 후 Azure OpenAI 답변을 생성합니다.")
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> createChat(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestHeader("X-Service-ID") String serviceId,
            @RequestHeader("X-Transaction-ID") String transactionId,
            @Valid @RequestBody ChatRequest request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }
        ChatResponse response = chatService.processChat(request.question(), authenticatedUser.userUuid());
        return ResponseEntity
                .status(SuccessStatus.CHAT_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.CHAT_SUCCESS, response));
    }
}