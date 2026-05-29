package com.woorifisa.won_card_channel_server.domain.ai.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.ai.invest.model.InvestSummary;
import com.woorifisa.won_card_channel_server.domain.ai.invest.repository.InvestSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.ai.openai.service.OpenAiService;
import com.woorifisa.won_card_channel_server.domain.ai.openai.service.OpenAiService.ClassifyResult;
import com.woorifisa.won_card_channel_server.domain.ai.openai.service.OpenAiService.GenerateResult;
import com.woorifisa.won_card_channel_server.domain.ai.spend.model.SpendSummary;
import com.woorifisa.won_card_channel_server.domain.ai.spend.repository.SpendSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.ai.chat.dto.response.ChatResponse;
import com.woorifisa.won_card_channel_server.global.config.QueryIntent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final UUID USER_UUID =
            UUID.fromString("8976c015-14e7-4c82-8817-978434d353dc");

    @Mock private OpenAiService openAiService;
    @Mock private SpendSummaryRepository spendSummaryRepository;
    @Mock private InvestSummaryRepository investSummaryRepository;
    @Mock private Driver cardNeo4jDriver;
    @Mock private Driver securitiesNeo4jDriver;
    @Mock private Session cardSession;
    @Mock private Session securitiesSession;
    @Mock private Result neo4jResult;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                openAiService,
                spendSummaryRepository,
                investSummaryRepository,
                new ObjectMapper(),
                cardNeo4jDriver,
                securitiesNeo4jDriver
        );
    }

    @Test
    @DisplayName("카드 MySQL 의도로 분류되면 SpendSummary를 조회하고 답변을 생성한다")
    void processChat_카드MySQL의도_성공() {
        // given
        given(openAiService.classifyIntent("이번 달 식비 알려줘"))
                .willReturn(new ClassifyResult(QueryIntent.FOOD_SPEND, 0.92, Map.of("month", "2026-05")));
        given(spendSummaryRepository.findLatestByUserUuid(USER_UUID))
                .willReturn(Optional.of(spendSummary()));
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.FOOD_SPEND), anyString()))
                .willReturn(new GenerateResult("이번 달 식비는 45,200원입니다.", List.of("지난달이랑 비교해줘")));

        // when
        ChatResponse response = chatService.processChat("이번 달 식비 알려줘", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("이번 달 식비는 45,200원입니다.");
        assertThat(response.contextUsed()).containsExactly("CARD_TRANSACTION");
        assertThat(response.suggestedQuestions()).containsExactly("지난달이랑 비교해줘");
        then(investSummaryRepository).shouldHaveNoInteractions();
        then(cardNeo4jDriver).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("증권 MySQL 의도로 분류되면 InvestSummary와 증권 Neo4j를 함께 조회하고 답변을 생성한다")
    @SuppressWarnings("unchecked")
    void processChat_증권MySQL의도_성공() {
        // given
        given(openAiService.classifyIntent("내 ETF 수익률 알려줘"))
                .willReturn(new ClassifyResult(QueryIntent.ETF_HOLDINGS, 0.88, Map.of()));
        given(investSummaryRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(investSummary()));
        given(securitiesNeo4jDriver.session()).willReturn(securitiesSession);
        given(securitiesSession.run(anyString(), any(Map.class))).willReturn(neo4jResult);
        given(neo4jResult.list(any())).willReturn(
                List.of(Map.of("etfName", "TIGER 200", "quantity", 5L, "currentValue", 520000L))
        );
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.ETF_HOLDINGS), anyString()))
                .willReturn(new GenerateResult("현재 ETF 보유 현황입니다.", List.of("수익률 비교해줘")));

        // when
        ChatResponse response = chatService.processChat("내 ETF 수익률 알려줘", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("현재 ETF 보유 현황입니다.");
        assertThat(response.contextUsed()).containsExactlyInAnyOrder("INVESTMENT_DATA", "SECURITIES_GRAPH");
        then(spendSummaryRepository).shouldHaveNoInteractions();
        then(cardNeo4jDriver).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Neo4j 의도로 분류되면 그래프 DB를 조회하고 답변을 생성한다")
    @SuppressWarnings("unchecked")
    void processChat_Neo4j의도_성공() {
        // given
        given(openAiService.classifyIntent("카테고리별 지출 분석해줘"))
                .willReturn(new ClassifyResult(QueryIntent.SPEND_BY_CATEGORY, 0.90, Map.of()));
        given(cardNeo4jDriver.session()).willReturn(cardSession);
        given(cardSession.run(anyString(), any(Map.class))).willReturn(neo4jResult);
        given(neo4jResult.list(any())).willReturn(
                List.of(Map.of("category", "음식", "totalAmount", 45200L))
        );
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.SPEND_BY_CATEGORY), anyString()))
                .willReturn(new GenerateResult("카테고리별 지출 분석 결과입니다.", List.of("음식 지출 더 알려줘")));

        // when
        ChatResponse response = chatService.processChat("카테고리별 지출 분석해줘", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("카테고리별 지출 분석 결과입니다.");
        assertThat(response.contextUsed()).containsExactly("CARD_GRAPH");
        then(spendSummaryRepository).shouldHaveNoInteractions();
        then(investSummaryRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("UNKNOWN 의도로 분류되면 DB 조회 없이 답변을 생성한다")
    void processChat_UNKNOWN의도_DB조회없음() {
        // given
        given(openAiService.classifyIntent("안녕"))
                .willReturn(new ClassifyResult(QueryIntent.UNKNOWN, 0.95, Map.of()));
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.UNKNOWN), eq(null)))
                .willReturn(new GenerateResult("안녕하세요! 금융 관련 질문을 해주세요.", List.of("이번 달 지출 알려줘")));

        // when
        ChatResponse response = chatService.processChat("안녕", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("안녕하세요! 금융 관련 질문을 해주세요.");
        assertThat(response.contextUsed()).isEmpty();
        then(spendSummaryRepository).shouldHaveNoInteractions();
        then(investSummaryRepository).shouldHaveNoInteractions();
        then(cardNeo4jDriver).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("신뢰도가 0.7 미만이면 재질문 유도 응답을 반환하고 DB와 AI 답변 생성을 호출하지 않는다")
    void processChat_낮은신뢰도_재질문유도() {
        // given
        given(openAiService.classifyIntent("지출"))
                .willReturn(new ClassifyResult(QueryIntent.FOOD_SPEND, 0.50, Map.of()));

        // when
        ChatResponse response = chatService.processChat("지출", USER_UUID);

        // then
        assertThat(response.answer()).contains("더 구체적으로 입력해 주세요");
        assertThat(response.contextUsed()).isEmpty();
        assertThat(response.suggestedQuestions()).isEmpty();
        then(spendSummaryRepository).shouldHaveNoInteractions();
        then(investSummaryRepository).shouldHaveNoInteractions();
        then(openAiService).should(never()).generateResponse(any(), any(), any());
    }

    @Test
    @DisplayName("신뢰도가 정확히 0.7이면 정상 처리한다")
    void processChat_신뢰도경계값_0_7_정상처리() {
        // given
        given(openAiService.classifyIntent("식비"))
                .willReturn(new ClassifyResult(QueryIntent.FOOD_SPEND, 0.7, Map.of()));
        given(spendSummaryRepository.findLatestByUserUuid(USER_UUID))
                .willReturn(Optional.empty());
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.FOOD_SPEND), eq(null)))
                .willReturn(new GenerateResult("데이터를 찾을 수 없습니다.", List.of()));

        // when
        ChatResponse response = chatService.processChat("식비", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("데이터를 찾을 수 없습니다.");
        then(openAiService).should().generateResponse(anyString(), eq(QueryIntent.FOOD_SPEND), eq(null));
    }

    @Test
    @DisplayName("DB 조회 중 예외가 발생해도 서버 에러 없이 partial 응답을 반환한다")
    void processChat_DB조회실패_partial응답() {
        // given
        given(openAiService.classifyIntent("이번 달 총 지출"))
                .willReturn(new ClassifyResult(QueryIntent.TOTAL_SPEND, 0.91, Map.of()));
        given(spendSummaryRepository.findLatestByUserUuid(USER_UUID))
                .willThrow(new RuntimeException("DB 연결 실패"));
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.TOTAL_SPEND), eq(null)))
                .willReturn(new GenerateResult("일시적으로 데이터를 조회할 수 없습니다.", List.of("잠시 후 다시 시도해줘")));

        // when
        ChatResponse response = chatService.processChat("이번 달 총 지출", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("일시적으로 데이터를 조회할 수 없습니다.");
        assertThat(response.contextUsed()).isEmpty();
        then(openAiService).should().generateResponse(anyString(), eq(QueryIntent.TOTAL_SPEND), eq(null));
    }

    @Test
    @DisplayName("증권 MySQL 조회 실패해도 증권 Neo4j 조회에 성공하면 partial 응답을 반환한다")
    @SuppressWarnings("unchecked")
    void processChat_증권MySQL실패_Neo4j성공_partial응답() {
        // given
        given(openAiService.classifyIntent("내 ETF 보유 현황"))
                .willReturn(new ClassifyResult(QueryIntent.ETF_HOLDINGS, 0.85, Map.of()));
        given(investSummaryRepository.findByUserUuid(USER_UUID))
                .willThrow(new RuntimeException("MySQL 연결 실패"));
        given(securitiesNeo4jDriver.session()).willReturn(securitiesSession);
        given(securitiesSession.run(anyString(), any(Map.class))).willReturn(neo4jResult);
        given(neo4jResult.list(any())).willReturn(
                List.of(Map.of("etfName", "TIGER 200", "quantity", 5L, "currentValue", 520000L))
        );
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.ETF_HOLDINGS), anyString()))
                .willReturn(new GenerateResult("그래프 기반 ETF 보유 현황입니다.", List.of("수익률 알려줘")));

        // when
        ChatResponse response = chatService.processChat("내 ETF 보유 현황", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("그래프 기반 ETF 보유 현황입니다.");
        assertThat(response.contextUsed()).containsExactly("SECURITIES_GRAPH");
        then(spendSummaryRepository).shouldHaveNoInteractions();
        then(cardNeo4jDriver).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("증권 Neo4j 조회 실패해도 증권 MySQL 조회에 성공하면 partial 응답을 반환한다")
    void processChat_증권Neo4j실패_MySQL성공_partial응답() {
        // given
        given(openAiService.classifyIntent("내 ETF 보유 현황"))
                .willReturn(new ClassifyResult(QueryIntent.ETF_HOLDINGS, 0.85, Map.of()));
        given(investSummaryRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(investSummary()));
        given(securitiesNeo4jDriver.session()).willThrow(new RuntimeException("Neo4j 연결 실패"));
        given(openAiService.generateResponse(anyString(), eq(QueryIntent.ETF_HOLDINGS), anyString()))
                .willReturn(new GenerateResult("MySQL 기반 ETF 보유 현황입니다.", List.of("수익률 알려줘")));

        // when
        ChatResponse response = chatService.processChat("내 ETF 보유 현황", USER_UUID);

        // then
        assertThat(response.answer()).isEqualTo("MySQL 기반 ETF 보유 현황입니다.");
        assertThat(response.contextUsed()).containsExactly("INVESTMENT_DATA");
        then(spendSummaryRepository).shouldHaveNoInteractions();
        then(cardNeo4jDriver).shouldHaveNoInteractions();
    }

    private SpendSummary spendSummary() {
        return SpendSummary.builder()
                .userUuid(USER_UUID)
                .baseMonth("2026-05")
                .totalSpendAmount(150000L)
                .foodAmount(45200L)
                .shoppingAmount(30000L)
                .transportAmount(25000L)
                .subscriptionAmount(15000L)
                .etcAmount(34800L)
                .topMerchantJson("{}")
                .pointBalanceAmount(3500L)
                .currentMonthSpendAmount(150000L)
                .rewardRate(1.0)
                .performanceStatus("2")
                .build();
    }

    private InvestSummary investSummary() {
        return InvestSummary.builder()
                .userUuid(USER_UUID)
                .investAccountUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .totalBuyAmount(500000L)
                .profitLossAmount(25000L)
                .etfSummaryJson("[]")
                .build();
    }
}
