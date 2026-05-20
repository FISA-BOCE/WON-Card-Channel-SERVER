package com.woorifisa.won_card_channel_server.domain.reward.mapper;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RewardLedgerMapperTest {
    private final RewardLedgerMapper rewardLedgerMapper = new RewardLedgerMapper();

    @Test
    @DisplayName("Card Core 리워드 내역 응답을 Card Channel 응답으로 변환한다")
    void toResponse() {
        // given
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 7, 14, 32);

        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(
                2026,
                1245000L,
                List.of(
                        new CardCoreRewardLedgerResponse.CardCoreRewardLedgerItem(
                                1001L,
                                "2026-05",
                                12450L,
                                "EARN",
                                occurredAt
                        )
                )
        );

        // when
        RewardLedgerResponse response = rewardLedgerMapper.toResponse(coreResponse);

        // then
        assertThat(response.baseYear()).isEqualTo(2026);
        assertThat(response.totalAccumulatedAmount()).isEqualTo(1245000L);
        assertThat(response.ledgers()).hasSize(1);

        RewardLedgerResponse.RewardLedgerItem item = response.ledgers().get(0);

        assertThat(item.pointLedgerId()).isEqualTo(1001L);
        assertThat(item.baseMonth()).isEqualTo("2026-05");
        assertThat(item.pointAmount()).isEqualTo(12450L);
        assertThat(item.type()).isEqualTo("EARN");
        assertThat(item.occurredAt()).isEqualTo(occurredAt);

    }

    @Test
    @DisplayName("Card Core 리워드 내역이 비어 있으면 빈 목록으로 변환한다")
    void toResponseEmptyLedgers() {
        // given
        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(
                2026,
                0L,
                List.of()
        );

        // when
        RewardLedgerResponse response = rewardLedgerMapper.toResponse(coreResponse);

        // then
        assertThat(response.baseYear()).isEqualTo(2026);
        assertThat(response.totalAccumulatedAmount()).isEqualTo(0L);
        assertThat(response.ledgers()).isEmpty();
    }

}
