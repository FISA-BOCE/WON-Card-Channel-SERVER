package com.woorifisa.won_card_channel_server.domain.aidb.api;

import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.AiDbQueryResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.service.AiDbQueryService;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/card/db")
@Tag(name = "Internal AI DB Query API", description = "AI 서버로 부터 DB 조회 요청을 수행합니다.")
public class AiDbQueryApi {

    private final AiDbQueryService aiDbQueryService;

    @Operation(summary = "Mysql에 쿼리하여 데이터를 불러와 AI 서버에 전달합니다.")
    @PostMapping("/mysql/query")
    public ResponseEntity<ApiResponse<AiDbQueryResponse<?>>> query(
            @Valid @RequestBody AiDbQueryRequest request
    ) {
        AiDbQueryResponse<?> response = aiDbQueryService.query(request);

        return ResponseEntity
                .status(SuccessStatus.OK.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.OK, response));
    }
}
