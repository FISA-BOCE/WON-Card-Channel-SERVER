package com.woorifisa.won_card_channel_server.domain.reward.mapper;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerDetailResponse;
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
                List.of(new CardCoreRewardLedgerResponse.CardCoreRewardLedgerItem(
                        1001L,
                        "2026-05",
                        12450L,
                        "EARN",
                        "FAILED",
                        "SWEEP_FAIL_008",
                        "매수 가능한 금액이 부족합니다.",
                        occurredAt
                ))
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
        assertThat(item.sweepStatus()).isEqualTo("FAILED");
        assertThat(item.sweepFailureCode()).isEqualTo("SWEEP_FAIL_008");
        assertThat(item.sweepFailureMessage()).isEqualTo("매수 가능한 금액이 부족합니다.");
        assertThat(item.occurredAt()).isEqualTo(occurredAt);

    }

    @Test
    @DisplayName("Card Core 리워드 내역이 비어 있으면 빈 목록으로 변환한다")
    void toResponseEmptyLedgers() {
        // given
        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(2026, 0L, List.of());

        // when
        RewardLedgerResponse response = rewardLedgerMapper.toResponse(coreResponse);

        // then
        assertThat(response.baseYear()).isEqualTo(2026);
        assertThat(response.totalAccumulatedAmount()).isEqualTo(0L);
        assertThat(response.ledgers()).isEmpty();
    }

    @Test
    @DisplayName("Card Core 응답의 ledgers가 null이면 빈 목록으로 변환한다")
    void toResponseNullLedgers() {
        // given
        CardCoreRewardLedgerResponse coreResponse = new CardCoreRewardLedgerResponse(2026, 0L, null);

        // when
        RewardLedgerResponse response = rewardLedgerMapper.toResponse(coreResponse);

        // then
        assertThat(response.baseYear()).isEqualTo(2026);
        assertThat(response.totalAccumulatedAmount()).isEqualTo(0L);
        assertThat(response.ledgers()).isEmpty();
    }

    @Test
    @DisplayName("Core 리워드 상세 응답을 Channel 리워드 상세 응답으로 변환한다")
    void toDetailResponse() {
        // given
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 7, 14, 32);

        CardCoreRewardLedgerDetailResponse.CardCoreRewardDetail coreDetail =
                new CardCoreRewardLedgerDetailResponse.CardCoreRewardDetail(820000L, 500000L, 0L);

        CardCoreRewardLedgerDetailResponse coreResponse =
                new CardCoreRewardLedgerDetailResponse(
                        1L,
                        "2026-05",
                        "EARN",
                        12450L,
                        "FAILED",
                        "SWEEP_FAIL_008",
                        "매수 가능한 금액이 부족합니다.",
                        occurredAt,
                        coreDetail
                );

        // when
        RewardLedgerDetailResponse response = rewardLedgerMapper.toDetailResponse(coreResponse);

        // then
        assertThat(response.pointLedgerId()).isEqualTo(1L);
        assertThat(response.baseMonth()).isEqualTo("2026-05");
        assertThat(response.type()).isEqualTo("EARN");
        assertThat(response.pointAmount()).isEqualTo(12450L);
        assertThat(response.sweepStatus()).isEqualTo("FAILED");
        assertThat(response.sweepFailureCode()).isEqualTo("SWEEP_FAIL_008");
        assertThat(response.sweepFailureMessage()).isEqualTo("매수 가능한 금액이 부족합니다.");
        assertThat(response.occurredAt()).isEqualTo(occurredAt);

        RewardLedgerDetailResponse.RewardDetail detail = response.detail();

        assertThat(detail.previousMonthSpendAmount()).isEqualTo(820000L);
        assertThat(detail.targetSpendAmount()).isEqualTo(500000L);
        assertThat(detail.shortfallAmount()).isEqualTo(0L);
    }
}
