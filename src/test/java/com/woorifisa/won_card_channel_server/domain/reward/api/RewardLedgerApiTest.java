package com.woorifisa.won_card_channel_server.domain.reward.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.service.RewardLedgerService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RewardLedgerApi.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "internal.card-core.url=http://localhost:8082"
})
class RewardLedgerApiTest {

    private static final UUID CARD_USER_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardLedgerService rewardLedgerService;

    @Test
    @DisplayName("자동투자 리워드 내역을 조회한다")
    void getRewardLedger() throws Exception {
        // given
        RewardLedgerResponse response = new RewardLedgerResponse(
                2026,
                1245000L,
                List.of(
                        new RewardLedgerResponse.RewardLedgerItem(
                                1001L,
                                "2026-05",
                                12450L,
                                "EARN",
                                LocalDateTime.of(2026, 5, 7, 14, 32)
                        )
                )
        );

        given(rewardLedgerService.getRewardLedger(any(UUID.class), anyString()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/cards/rewards/ledger")
                                .param("type", "EARN")
                                .header("Authorization", "Bearer test-token")
                                .header("X-Card-User-UUID", CARD_USER_UUID.toString())
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
                .andExpect(jsonPath("$.data.ledgers[0].type").value("EARN"));
    }

    @Test
    @DisplayName("type 파라미터 없이 리워드 내역을 조회한다")
    void getRewardLedgerWithoutType() throws Exception {
        // given
        RewardLedgerResponse response = new RewardLedgerResponse(
                2026,
                1245000L,
                List.of()
        );

        given(rewardLedgerService.getRewardLedger(any(UUID.class), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/cards/rewards/ledger")
                                .header("Authorization", "Bearer test-token")
                                .header("X-Card-User-UUID", CARD_USER_UUID.toString())
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

}
