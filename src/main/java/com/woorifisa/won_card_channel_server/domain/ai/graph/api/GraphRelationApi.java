package com.woorifisa.won_card_channel_server.domain.ai.graph.api;

import com.woorifisa.won_card_channel_server.domain.ai.graph.dto.response.GraphRelationResponse;
import com.woorifisa.won_card_channel_server.domain.ai.graph.service.GraphRelationService;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Graph", description = "그래프 관계 데이터 API")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class GraphRelationApi {

    private final GraphRelationService graphRelationService;

    @Operation(
            summary = "관계 데이터 조회",
            description = "카드 결제 → 포인트 → ETF 관계를 Neo4j 그래프로 탐색하여 nodes/links 구조로 반환합니다."
    )
    @GetMapping("/graph/relations")
    public ResponseEntity<ApiResponse<GraphRelationResponse>> getGraphRelations(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestHeader("X-Service-ID") String serviceId,
            @RequestHeader("X-Transaction-ID") String transactionId,
            @RequestParam String queryType,
            @RequestParam(defaultValue = "1MONTH") String period,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) String category
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }
        GraphRelationResponse response = graphRelationService.getGraphRelations(
                authenticatedUser.userUuid(), queryType, period, merchant, category);
        return ResponseEntity
                .status(SuccessStatus.GRAPH_RELATION_SUCCESS.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.GRAPH_RELATION_SUCCESS, response));
    }
}