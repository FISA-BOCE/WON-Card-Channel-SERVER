package com.woorifisa.won_card_channel_server.domain.card.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardApplicationCreateRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationCreateResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationInvestAccountsResponse;
import com.woorifisa.won_card_channel_server.domain.card.service.CardApplicationInvestAccountService;
import com.woorifisa.won_card_channel_server.domain.card.service.CardApplicationService;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardApplicationApi.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CardApplicationApiTest {

    private static final UUID USER_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID AUTH_USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CARD_UUID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID INVEST_ACCOUNT_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardApplicationService cardApplicationService;

    @MockitoBean
    private CardApplicationInvestAccountService cardApplicationInvestAccountService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 없이 카드 신청용 증권 계좌 목록 API를 호출하면 401을 반환한다")
    void getInvestAccountsWithoutAuthentication() throws Exception {
        given(cardApplicationInvestAccountService.getInvestAccounts(isNull()))
                .willThrow(new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED));

        mockMvc.perform(get("/api/cards/applications/invest-accounts")
                        .header("X-Transaction-ID", "TX-20260604-CARD-INVEST01"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_002"));
    }

    @Test
    @DisplayName("카드 신청용 증권 계좌 목록 API는 200과 계좌 목록을 반환한다")
    void getInvestAccounts() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(toAuthentication(authenticatedUser()));

        given(cardApplicationInvestAccountService.getInvestAccounts(any(AuthenticatedUser.class)))
                .willReturn(new CardApplicationInvestAccountsResponse(List.of(
                        new CardApplicationInvestAccountsResponse.Account(
                                INVEST_ACCOUNT_UUID,
                                "123-***-***456",
                                true
                        )
                )));

        mockMvc.perform(get("/api/cards/applications/invest-accounts")
                        .header("X-Transaction-ID", "TX-20260604-CARD-INVEST02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CARD_200_003"))
                .andExpect(jsonPath("$.message").value("증권 계좌 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.accounts[0].invstAccountUuid").value(INVEST_ACCOUNT_UUID.toString()))
                .andExpect(jsonPath("$.data.accounts[0].accountNoDisplay").value("123-***-***456"))
                .andExpect(jsonPath("$.data.accounts[0].isLinked").value(true));
    }

    @Test
    @DisplayName("카드 신청 API는 201과 발급 결과를 반환한다")
    void applyCard() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(toAuthentication(authenticatedUser()));

        CardApplicationCreateRequest request = new CardApplicationCreateRequest(
                new CardApplicationCreateRequest.ApplicantInfo(
                        "홍길동",
                        "HONG GIL DONG",
                        "19900101",
                        "M",
                        "KR",
                        "01012345678",
                        "test@example.com",
                        "서울시 마포구 상암동",
                        "직장인"
                ),
                INVEST_ACCOUNT_UUID,
                1001L,
                "VOO",
                true,
                new CardApplicationCreateRequest.OptionalTerms(false, false)
        );

        given(cardApplicationService.applyCard(any(AuthenticatedUser.class), any(CardApplicationCreateRequest.class)))
                .willReturn(new CardApplicationCreateResponse(
                        CARD_UUID,
                        "****-****-****-1234",
                        LocalDateTime.of(2026, 5, 28, 17, 0),
                        "ACTIVE",
                        "S&P 500 ETF"
                ));

        mockMvc.perform(post("/api/cards/applications")
                        .contentType(APPLICATION_JSON)
                        .header("X-Transaction-ID", "TX-20260528-CARD-APP01")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CARD_201_001"))
                .andExpect(jsonPath("$.message").value("카드 신청이 완료되었습니다."))
                .andExpect(jsonPath("$.data.cardUuid").value(CARD_UUID.toString()))
                .andExpect(jsonPath("$.data.autoInvestEtfName").value("S&P 500 ETF"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTH_USER_UUID, USER_UUID, "test-jti");
    }

    private Authentication toAuthentication(AuthenticatedUser authenticatedUser) {
        return new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());
    }
}
