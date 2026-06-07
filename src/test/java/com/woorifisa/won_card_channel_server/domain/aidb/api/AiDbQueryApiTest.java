package com.woorifisa.won_card_channel_server.domain.aidb.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.request.AiDbQueryRequest;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.AiDbQueryResponse;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.AiDbQueryType;
import com.woorifisa.won_card_channel_server.domain.aidb.dto.response.CardMonthlyTotalSpendResult;
import com.woorifisa.won_card_channel_server.domain.aidb.exception.AiDbErrorCode;
import com.woorifisa.won_card_channel_server.domain.aidb.service.AiDbQueryService;
import com.woorifisa.won_card_channel_server.domain.aidb.service.Neo4jQueryService;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.exception.handler.GlobalExceptionHandler;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AiDbQueryApiTest {

    @Mock
    private AiDbQueryService aiDbQueryService;

    @Mock
    private Neo4jQueryService neo4jQueryService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiDbQueryApi(aiDbQueryService, neo4jQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /internal/card/db/mysql/query returns query result")
    void querySuccess() throws Exception {
        CardMonthlyTotalSpendResult result = new CardMonthlyTotalSpendResult(
                "2025-06",
                decimal("1000000"),
                decimal("200000"),
                decimal("500000"),
                decimal("100000"),
                decimal("100000"),
                decimal("100000")
        );
        doReturn(new AiDbQueryResponse<>(AiDbQueryType.CARD_MONTHLY_TOTAL_SPEND, result))
                .when(aiDbQueryService)
                .query(any(AiDbQueryRequest.class));

        mockMvc.perform(post("/internal/card/db/mysql/query")
                        .header("X-Service-ID", "test-ai-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("CARD_MONTHLY_TOTAL_SPEND", "2025-06")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COM_200_001"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.queryType").value("CARD_MONTHLY_TOTAL_SPEND"))
                .andExpect(jsonPath("$.data.result.baseMonth").value("2025-06"))
                .andExpect(jsonPath("$.data.result.totalSpendAmount").value(1000000))
                .andExpect(jsonPath("$.data.result.foodAmount").value(200000))
                .andExpect(jsonPath("$.data.result.shoppingAmount").value(500000))
                .andExpect(jsonPath("$.data.result.transportAmount").value(100000))
                .andExpect(jsonPath("$.data.result.subscriptionAmount").value(100000))
                .andExpect(jsonPath("$.data.result.etcAmount").value(100000));

        ArgumentCaptor<AiDbQueryRequest> captor = ArgumentCaptor.forClass(AiDbQueryRequest.class);
        verify(aiDbQueryService).query(captor.capture());
    }

    @Test
    @DisplayName("BusinessException is rendered as error response")
    void queryBusinessException() throws Exception {
        given(aiDbQueryService.query(any(AiDbQueryRequest.class)))
                .willThrow(new BusinessException(AiDbErrorCode.UNSUPPORTED_QUERY_TYPE));

        mockMvc.perform(post("/internal/card/db/mysql/query")
                        .header("X-Service-ID", "test-ai-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("UNKNOWN_QUERY", "2025-06")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("AIDB_400_003"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 queryType입니다."));
    }

    private String requestJson(String queryType, String baseMonth) throws Exception {
        return objectMapper.writeValueAsString(new AiDbQueryRequest(
                java.util.UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5"),
                queryType,
                new AiDbQueryRequest.Params(baseMonth)
        ));
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}


