package com.woorifisa.won_card_channel_server.domain.reward.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.service.RewardGetCurrentMonthService;
import com.woorifisa.won_card_channel_server.domain.reward.service.RewardLedgerService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RewardLedgerApi.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "internal.card-core.url=http://localhost:8082",
        "internal.invest-core.url=http://localhost:8083"
})
class RewardLedgerApiTest {

    private static final UUID USER_UUID =
            UUID.fromString("0a31e4b1-2b1d-4b5e-8b82-0fb48e502111");

    private static final UUID AUTH_USER_UUID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardLedgerService rewardLedgerService;

    @MockitoBean
    private RewardGetCurrentMonthService rewardGetCurrentMonthService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("자동투자 리워드 내역을 조회한다")
    void getRewardLedger() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();

        SecurityContextHolder.getContext()
                .setAuthentication(toAuthentication(authenticatedUser));

        RewardLedgerResponse response = new RewardLedgerResponse(
                2026,
                1245000L,
                List.of(
                        new RewardLedgerResponse.RewardLedgerItem(
                                1001L,
                                "2026-05",
                                12450L,
                                "EARN",
                                "FAILED",
                                "SWEEP_FAIL_008",
                                "매수 가능한 금액이 부족합니다.",
                                LocalDateTime.of(2026, 5, 7, 14, 32)
                        )
                )
        );

        given(rewardLedgerService.getRewardLedger(any(AuthenticatedUser.class), eq("EARN")))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/cards/rewards/ledger")
                                .param("type", "EARN")
                                .header("Authorization", "Bearer test-token")
                                .header("X-Service-ID", "WOORI-FISA-APP-01")
                                .header("X-Transaction-ID", "TX-20260512-RWD02")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리워드 내역 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.baseYear").value(2026))
                .andExpect(jsonPath("$.data.totalAccumulatedAmount").value(1245000))
                .andExpect(jsonPath("$.data.ledgers[0].pointLedgerId").value(1001))
                .andExpect(jsonPath("$.data.ledgers[0].baseMonth").value("2026-05"))
                .andExpect(jsonPath("$.data.ledgers[0].pointAmount").value(12450))
                .andExpect(jsonPath("$.data.ledgers[0].type").value("EARN"))
                .andExpect(jsonPath("$.data.ledgers[0].sweepStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.ledgers[0].sweepFailureCode").value("SWEEP_FAIL_008"))
                .andExpect(jsonPath("$.data.ledgers[0].sweepFailureMessage").value("매수 가능한 금액이 부족합니다."));
    }

    @Test
    @DisplayName("type 파라미터 없이 리워드 내역을 조회한다")
    void getRewardLedgerWithoutType() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();

        SecurityContextHolder.getContext()
                .setAuthentication(toAuthentication(authenticatedUser));

        RewardLedgerResponse response = new RewardLedgerResponse(
                2026,
                1245000L,
                List.of()
        );

        given(rewardLedgerService.getRewardLedger(any(AuthenticatedUser.class), isNull()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/cards/rewards/ledger")
                                .header("Authorization", "Bearer test-token")
                                .header("X-Service-ID", "WOORI-FISA-APP-01")
                                .header("X-Transaction-ID", "TX-20260512-RWD02")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리워드 내역 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.baseYear").value(2026))
                .andExpect(jsonPath("$.data.totalAccumulatedAmount").value(1245000))
                .andExpect(jsonPath("$.data.ledgers").isArray());
    }

    @Test
    @DisplayName("자동투자 리워드 상세 내역을 조회한다")
    void getRewardLedgerDetail() throws Exception {
        // given
        Long pointLedgerId = 1L;
        AuthenticatedUser authenticatedUser = authenticatedUser();

        RewardLedgerDetailResponse response = new RewardLedgerDetailResponse(
                pointLedgerId,
                "2026-05",
                "EARN",
                12450L,
                "FAILED",
                "SWEEP_FAIL_008",
                "매수 가능한 금액이 부족합니다.",
                LocalDateTime.of(2026, 5, 7, 14, 32),
                new RewardLedgerDetailResponse.RewardDetail(
                        820000L,
                        500000L,
                        0L
                )
        );

        given(rewardLedgerService.getRewardLedgerDetail(any(AuthenticatedUser.class), eq(pointLedgerId)
        )).willReturn(response);

        SecurityContextHolder.getContext()
                .setAuthentication(toAuthentication(authenticatedUser));

        // when & then
        mockMvc.perform(
                        get("/api/cards/rewards/ledger/{pointLedgerId}", pointLedgerId)
                                .header("Authorization", "Bearer test-token")
                                .header("X-Service-ID", "WOORI-FISA-APP-01")
                                .header("X-Transaction-ID", "TX-20260512-RWD03")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("상세 리워드 내역 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.pointLedgerId").value(1))
                .andExpect(jsonPath("$.data.baseMonth").value("2026-05"))
                .andExpect(jsonPath("$.data.type").value("EARN"))
                .andExpect(jsonPath("$.data.pointAmount").value(12450))
                .andExpect(jsonPath("$.data.sweepStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.sweepFailureCode").value("SWEEP_FAIL_008"))
                .andExpect(jsonPath("$.data.sweepFailureMessage").value("매수 가능한 금액이 부족합니다."))
                .andExpect(jsonPath("$.data.detail.previousMonthSpendAmount").value(820000))
                .andExpect(jsonPath("$.data.detail.targetSpendAmount").value(500000))
                .andExpect(jsonPath("$.data.detail.shortfallAmount").value(0));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                AUTH_USER_UUID,
                USER_UUID,
                "test-jti"
        );
    }

    private Authentication toAuthentication(AuthenticatedUser authenticatedUser) {
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of()
        );
    }

}
