package com.woorifisa.won_card_channel_server.domain.reward.service;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.exception.RewardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.external.CardCoreRewardApi;
import com.woorifisa.won_card_channel_server.domain.reward.mapper.RewardLedgerMapper;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RewardLedgerServiceTest {

    private static final UUID CARD_USER_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private CardCoreRewardApi cardCoreRewardApi;

    @Mock
    private RewardLedgerMapper rewardLedgerMapper;

    @InjectMocks
    private RewardLedgerService rewardLedgerService;

    @Test
    @DisplayName("자동투자 리워드 내역을 조회한다")
    void getRewardLedger() {
        // given
        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(
                2026,
                1245000L,
                List.of(
                        new CardCoreRewardLedgerResponse.CardCoreRewardLedgerItem(
                                1001L,
                                "2026-05",
                                12450L,
                                "EARN",
                                LocalDateTime.of(2026, 5, 7, 14, 32)
                        )
                )
        );

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

        given(cardCoreRewardApi.getRewardLedger(CARD_USER_UUID, "EARN"))
                .willReturn(ApiResponse.of(SuccessStatus.OK, coreResponse));

        given(rewardLedgerMapper.toResponse(coreResponse))
                .willReturn(response);

        // when
        RewardLedgerResponse result = rewardLedgerService.getRewardLedger(CARD_USER_UUID, "EARN");

        // then
        assertThat(result.baseYear()).isEqualTo(2026);
        assertThat(result.totalAccumulatedAmount()).isEqualTo(1245000L);
        assertThat(result.ledgers()).hasSize(1);
        assertThat(result.ledgers().get(0).pointLedgerId()).isEqualTo(1001L);
        assertThat(result.ledgers().get(0).baseMonth()).isEqualTo("2026-05");
        assertThat(result.ledgers().get(0).pointAmount()).isEqualTo(12450L);
        assertThat(result.ledgers().get(0).type()).isEqualTo("EARN");

        then(cardCoreRewardApi).should().getRewardLedger(CARD_USER_UUID, "EARN");
        then(rewardLedgerMapper).should().toResponse(coreResponse);
    }

    @Test
    @DisplayName("type 파라미터가 없으면 ALL로 조회한다")
    void getRewardLedgerWithoutType() {
        // given
        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(
                2026,
                1245000L,
                List.of()
        );

        RewardLedgerResponse response = new RewardLedgerResponse(
                2026,
                1245000L,
                List.of()
        );

        given(cardCoreRewardApi.getRewardLedger(CARD_USER_UUID, "ALL"))
                .willReturn(ApiResponse.of(SuccessStatus.OK, coreResponse));

        given(rewardLedgerMapper.toResponse(coreResponse))
                .willReturn(response);

        // when
        RewardLedgerResponse result = rewardLedgerService.getRewardLedger(CARD_USER_UUID, null);

        // then
        assertThat(result.baseYear()).isEqualTo(2026);
        assertThat(result.totalAccumulatedAmount()).isEqualTo(1245000L);
        assertThat(result.ledgers()).isEmpty();

        then(cardCoreRewardApi).should().getRewardLedger(CARD_USER_UUID, "ALL");
        then(rewardLedgerMapper).should().toResponse(coreResponse);
    }

    @Test
    @DisplayName("유효하지 않은 type 값이면 예외가 발생한다")
    void getRewardLedgerInvalidType() {
        // given
        String invalidType = "BAD";

        // when & then
        assertThatThrownBy(() -> rewardLedgerService.getRewardLedger(CARD_USER_UUID, invalidType))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode())
                            .isEqualTo(RewardErrorCode.INVALID_REWARD_LEDGER_TYPE);
                });
    }

}
