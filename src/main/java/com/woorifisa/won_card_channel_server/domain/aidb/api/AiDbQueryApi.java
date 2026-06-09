package com.woorifisa.won_card_channel_server.domain.aidb.api;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.AiDbQueryResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.Neo4jQueryResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.service.AiDbQueryService;
import com.woorifisa.won_card_channel_server.domain.aidb.service.Neo4jQueryService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/card/db")
@ConditionalOnProperty(
        prefix = "features",
        name = {"aidb.enabled", "neo4j.enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@Tag(name = "Internal AI DB Query API", description = "AI 서버로 부터 DB 조회 요청을 수행합니다.")
public class AiDbQueryApi {

    private final AiDbQueryService aiDbQueryService;
    private final Neo4jQueryService neo4jQueryService;

    @Operation(summary = "Mysql에 쿼리하여 데이터를 불러와 AI 서버에 전달합니다.")
    @PostMapping("/mysql/query")
    public ResponseEntity<ApiResponse<AiDbQueryResponse<?>>> query(
            @Parameter(description = "호출 서비스 식별자", required = true)
            @RequestHeader("X-Service-ID") String serviceId,
            @Valid @RequestBody AiDbQueryRequest request
    ) {
        AiDbQueryResponse<?> response = aiDbQueryService.query(request);

        return ResponseEntity
                .status(SuccessStatus.OK.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.OK, response));
    }

    @Operation(summary = "Query card Neo4j graph DB")
    @PostMapping("/graph/query")
    public ResponseEntity<ApiResponse<Neo4jQueryResponse>> queryGraph(
            @Parameter(description = "호출 서비스 식별자", required = true)
            @RequestHeader("X-Service-ID") String serviceId,
            @Valid @RequestBody AiDbQueryRequest request
    ) {
        Neo4jQueryResponse response = neo4jQueryService.query(request);

        return ResponseEntity
                .status(SuccessStatus.OK.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.OK, response));
    }
}
