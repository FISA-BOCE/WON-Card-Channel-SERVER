package com.woorifisa.won_card_channel_server.domain.card.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardInfoResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.ExistingCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.service.CardSummaryService;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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

@WebMvcTest(CardApi.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CardApiTest {

    private static final UUID USER_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID AUTH_USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardSummaryService cardSummaryService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 없이 카드 정보 조회 API를 호출하면 401을 반환한다")
    void getCardInfoWithoutAuthentication() throws Exception {
        given(cardSummaryService.getCardInfo(isNull()))
                .willThrow(new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED));

        mockMvc.perform(get("/api/cards/info")
                        .header("X-Transaction-ID", "TX-20260610-CARD-INFO01"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_002"));
    }

    @Test
    @DisplayName("카드 미보유 사용자는 빈 배열을 반환한다")
    void getCardInfoWithoutIssuedCard() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(toAuthentication(authenticatedUser()));

        given(cardSummaryService.getCardInfo(any(AuthenticatedUser.class)))
                .willReturn(new CardInfoResponse(List.of()));

        mockMvc.perform(get("/api/cards/info")
                        .header("X-Transaction-ID", "TX-20260610-CARD-INFO02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CARD_200_001"))
                .andExpect(jsonPath("$.message").value("카드 정보 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.cards").isArray())
                .andExpect(jsonPath("$.data.cards").isEmpty());
    }

    @Test
    @DisplayName("카드 보유 사용자는 카드명과 마스킹 카드 번호를 반환한다")
    void getCardInfoWithIssuedCard() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(toAuthentication(authenticatedUser()));

        given(cardSummaryService.getCardInfo(any(AuthenticatedUser.class)))
                .willReturn(new CardInfoResponse(List.of(
                        new CardInfoResponse.CardInfo(
                                "WON 자동투자 카드",
                                "**** **** **** 1234"
                        )
                )));

        mockMvc.perform(get("/api/cards/info")
                        .header("X-Transaction-ID", "TX-20260610-CARD-INFO03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CARD_200_001"))
                .andExpect(jsonPath("$.data.cards[0].cardName").value("WON 자동투자 카드"))
                .andExpect(jsonPath("$.data.cards[0].cardNoDisplay").value("**** **** **** 1234"));
    }

    @Test
    @DisplayName("기존 카드 요약 API는 카드 보유 여부에 맞는 응답을 반환한다")
    void getCards() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(toAuthentication(authenticatedUser()));

        given(cardSummaryService.getCards(any(AuthenticatedUser.class)))
                .willReturn(new ExistingCardSummaryResponse(
                        "55555555-5555-5555-5555-555555555555",
                        "WON 자동투자 카드",
                        "**** **** **** 1234",
                        "ACTIVE",
                        new ExistingCardSummaryResponse.UsageSummary(
                                BigDecimal.valueOf(100_000L),
                                List.of(),
                                BigDecimal.valueOf(0.7),
                                0L,
                                500_000L
                        )
                ));

        mockMvc.perform(get("/api/cards")
                        .header("X-Transaction-ID", "TX-20260610-CARD-INFO04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CARD_200_001"))
                .andExpect(jsonPath("$.data.cardName").value("WON 자동투자 카드"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTH_USER_UUID, USER_UUID, "test-jti");
    }

    private Authentication toAuthentication(AuthenticatedUser authenticatedUser) {
        return new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());
    }
}
