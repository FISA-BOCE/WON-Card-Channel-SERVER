package com.woorifisa.won_card_channel_server.domain.autoinvest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.request.AutoInvestSubscriptionChangeRequest;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionChangeResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionDetailResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.AutoInvestSubscriptionService;
import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import com.woorifisa.won_card_channel_server.global.config.SecurityConfig;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.global.security.JwtTokenProvider;
import com.woorifisa.won_card_channel_server.global.security.RestAccessDeniedHandler;
import com.woorifisa.won_card_channel_server.global.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutoInvestSubscriptionApi.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "internal.invest-core.url=http://localhost:8083"
})
class AutoInvestSubscriptionApiTest {

    private static final UUID USER_UUID =
            UUID.fromString("248bc5f2-f0a0-4c51-9d62-168c87cb7ab7");
    private static final UUID AUTH_USER_UUID =
            UUID.fromString("4810c4fd-3530-4f9d-bf1c-146f50e652cc");
    private static final UUID CARD_UUID =
            UUID.fromString("188340e6-0205-44df-a4dc-8c8db40b64c6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutoInvestSubscriptionService autoInvestSubscriptionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("인증 없이 자동투자 설정 조회 API를 호출하면 401을 반환한다")
    void getSubscriptionWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/cards/{cardUuid}/auto-invest", CARD_UUID)
                        .header("X-Service-ID", "WOORI-WON-APP")
                        .header("X-Transaction-ID", "TX-20260528-SUB01"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_002"));

        verify(autoInvestSubscriptionService, never()).getSubscription(any(AuthenticatedUser.class), any(UUID.class));
    }

    @Test
    @DisplayName("인증된 사용자의 자동투자 설정 조회 API는 200과 현재 설정 데이터를 반환한다")
    void getSubscription() throws Exception {
        AutoInvestSubscriptionDetailResponse response = new AutoInvestSubscriptionDetailResponse(
                CARD_UUID,
                new AutoInvestSubscriptionDetailResponse.CurrentEtf(
                        1001L,
                        "S&P 500 ETF",
                        "VOO",
                        LocalDateTime.of(2026, 5, 28, 12, 30)
                ),
                new AutoInvestSubscriptionDetailResponse.PendingEtf(
                        1002L,
                        "NASDAQ 100 ETF",
                        "QQQ",
                        LocalDateTime.of(2026, 6, 1, 0, 0)
                ),
                true
        );

        given(jwtTokenProvider.parse("test-token")).willReturn(authenticatedUser());
        given(tokenBlacklistService.isBlacklisted("test-jti")).willReturn(false);
        given(autoInvestSubscriptionService.getSubscription(any(AuthenticatedUser.class), any(UUID.class))).willReturn(response);

        mockMvc.perform(get("/api/cards/{cardUuid}/auto-invest", CARD_UUID)
                        .header("Authorization", "Bearer test-token")
                        .header("X-Service-ID", "WOORI-WON-APP")
                        .header("X-Transaction-ID", "TX-20260528-SUB02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTO_200_001"))
                .andExpect(jsonPath("$.message").value("자동투자 설정 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.cardUuid").value(CARD_UUID.toString()))
                .andExpect(jsonPath("$.data.currentEtf.ticker").value("VOO"))
                .andExpect(jsonPath("$.data.pendingEtf.ticker").value("QQQ"));

        verify(autoInvestSubscriptionService).getSubscription(
                argThat(user ->
                        AUTH_USER_UUID.equals(user.authUserUuid())
                                && USER_UUID.equals(user.userUuid())
                                && "test-jti".equals(user.jti())
                ),
                eq(CARD_UUID)
        );
    }

    @Test
    @DisplayName("자동투자 ETF 변경 API는 200과 변경 결과를 반환한다")
    void changeSubscription() throws Exception {
        AutoInvestSubscriptionChangeResponse response = new AutoInvestSubscriptionChangeResponse(
                CARD_UUID,
                new AutoInvestSubscriptionChangeResponse.PreviousEtf(
                        "S&P 500 ETF",
                        "VOO",
                        LocalDateTime.of(2026, 5, 28, 18, 0)
                ),
                new AutoInvestSubscriptionChangeResponse.NewEtf(
                        1002L,
                        "NASDAQ 100 ETF",
                        "QQQ",
                        LocalDateTime.of(2026, 6, 1, 0, 0)
                )
        );

        given(jwtTokenProvider.parse("test-token")).willReturn(authenticatedUser());
        given(tokenBlacklistService.isBlacklisted("test-jti")).willReturn(false);
        given(autoInvestSubscriptionService.changeSubscription(
                any(AuthenticatedUser.class),
                any(UUID.class),
                any(AutoInvestSubscriptionChangeRequest.class)
        )).willReturn(response);

        mockMvc.perform(patch("/api/cards/{cardUuid}/auto-invest", CARD_UUID)
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token")
                        .header("X-Service-ID", "WOORI-WON-APP")
                        .header("X-Transaction-ID", "TX-20260528-SUB03")
                        .content(objectMapper.writeValueAsString(new AutoInvestSubscriptionChangeRequest(1002L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTO_200_002"))
                .andExpect(jsonPath("$.message").value("ETF 변경이 완료되었습니다."))
                .andExpect(jsonPath("$.data.previousEtf.ticker").value("VOO"))
                .andExpect(jsonPath("$.data.newEtf.ticker").value("QQQ"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTH_USER_UUID, USER_UUID, "test-jti");
    }
}
